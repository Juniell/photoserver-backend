package ru.fbear

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.*
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import ru.fbear.Info.keyAlias
import ru.fbear.Info.keyPass
import ru.fbear.Info.photoLifeTime
import ru.fbear.Info.photoServerPhrase
import ru.fbear.Info.tgmToken
import ru.fbear.Info.vkConfirmCode
import ru.fbear.Info.vkGroupId
import ru.fbear.Info.vkSecret
import ru.fbear.Info.vkToken
import ru.fbear.plugins.configureRouting
import java.io.File
import java.security.KeyStore
import kotlin.concurrent.timer
import kotlin.system.exitProcess


val secrets = readDatabase()
const val photoDirectory = "photos"

fun main() {
    deleteOldFilesTask() // Запуск таймера на удаление старых файлов

    val keyStoreFile = File("keystore.jks")
    val keystore: KeyStore

        try {
            keystore = KeyStore.getInstance(keyStoreFile, secrets.keyPass.toCharArray())
        } catch (e: Exception) {
            println("File for keyStore \"keystore.jks\" not found")
            exitProcess(0)
        }
    val environment = applicationEngineEnvironment {
        log = LoggerFactory.getLogger("ktor.photoserver_bot")

        sslConnector(
            keyStore = keystore,
            keyAlias = secrets.keyAlias,
            keyStorePassword = { secrets.keyPass.toCharArray() },
            privateKeyPassword = { secrets.keyPass.toCharArray() }) {
            port = 443
            keyStorePath = keyStoreFile
        }

        module {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                })
            }
            configureRouting()
        }
    }

    embeddedServer(Netty, environment).start(wait = true)
}

fun readDatabase(): Secrets {
    Database.connect("jdbc:sqlite:info.db", driver = "org.sqlite.JDBC")

    transaction {
        SchemaUtils.create(Info) // if not exist
        commit()
    }

    return try {
        transaction {
            val row = Info.selectAll().single()

            Secrets(
                keyPass = row[keyPass],
                keyAlias = row[keyAlias],
                vkGroupId = row[vkGroupId],
                vkToken = row[vkToken],
                vkSecret = row[vkSecret],
                vkConfirmCode = row[vkConfirmCode],
                tgmToken = row[tgmToken],
                photoServerPhrase = row[photoServerPhrase],
                photoLifeTime = row[photoLifeTime]
            )
        }
    } catch (e: NoSuchElementException) {
        var keyPass: String? = null
        var keyAlias: String? = null
        var vkGroupId: Int? = null
        var vkToken: String? = null
        var vkSecret: String? = null
        var vkConfirmCode: String? = null
        var tgmToken: String? = null
        var photoServerPhrase: String? = null
        var photoLifeTime: Int? = null

        while (keyPass.isNullOrEmpty()) {
            println("Enter keyPass:")
            val input = readLine() ?: exitProcess(0)
            keyPass = input
        }
        while (keyAlias.isNullOrEmpty()) {
            println("Enter keyAlias:")
            val input = readLine() ?: exitProcess(0)
            keyAlias = input
        }
        while (vkGroupId == null) {
            println("Enter vkGroupId:")
            val input = readLine() ?: exitProcess(0)
            try {
                vkGroupId = input.toInt()
            } catch (e: NumberFormatException) {
                continue
            }
        }
        while (vkToken.isNullOrEmpty()) {
            println("Enter vkToken:")
            val input = readLine() ?: exitProcess(0)
            vkToken = input
        }
        while (vkSecret.isNullOrEmpty()) {
            println("Enter vkSecret:")
            val input = readLine() ?: exitProcess(0)
            vkSecret = input
        }
        while (vkConfirmCode.isNullOrEmpty()) {
            println("Enter vkConfirmCode:")
            val input = readLine() ?: exitProcess(0)
            vkConfirmCode = input
        }
        while (tgmToken.isNullOrEmpty()) {
            println("Enter tgmToken:")
            val input = readLine() ?: exitProcess(0)
            tgmToken = input
        }
        while (photoServerPhrase.isNullOrEmpty()) {
            println("Enter phrase for photoServer:")
            val input = readLine() ?: exitProcess(0)
            photoServerPhrase = input
        }
        while (photoLifeTime == null) {
            println("Enter life time of photos (days):")
            val input = readLine() ?: exitProcess(0)
            try {
                photoLifeTime = input.toInt()
            } catch (e: NumberFormatException) {
                continue
            }
        }

        transaction {
            Info.insert {
                it[Info.keyPass] = keyPass
                it[Info.keyAlias] = keyAlias
                it[Info.vkGroupId] = vkGroupId
                it[Info.vkToken] = vkToken
                it[Info.vkSecret] = vkSecret
                it[Info.vkConfirmCode] = vkConfirmCode
                it[Info.tgmToken] = tgmToken
                it[Info.photoServerPhrase] = photoServerPhrase
                it[Info.photoLifeTime] = photoLifeTime
            }
            commit()
        }

        println("Attention: all photos older than $photoLifeTime days will be automatically deleted. Start server?")
        while (true) {
            println("Enter Y to start server, or N to stop")
            val input = readLine() ?: exitProcess(0)
            if (input == "Y") {
                println("Server starts")
                break
            }
            if (input == "N")
                exitProcess(0)
        }

        Secrets(
            keyPass,
            keyAlias,
            vkGroupId,
            vkToken,
            vkSecret,
            vkConfirmCode,
            tgmToken,
            photoServerPhrase,
            photoLifeTime
        )
    }
}

private fun deleteOldFilesTask() {
    timer(null, true, 1000L, 24 * 60 * 60 * 1000L) {    // каждые сутки
        val currentTime = System.currentTimeMillis()
        val oldTime = currentTime - secrets.photoLifeTime * 24 * 60 * 60 * 1000L

        val photos = File(photoDirectory).listFiles()

        if (photos.isNullOrEmpty())
            return@timer

        photos.forEach { photo ->
            if (photo != null && photo.exists() && photo.isFile && photo.lastModified() < oldTime) {
                println("Удаляю файл ${photo.name}")
                photo.delete()
            }
        }
    }
}

object Info : Table() {
    val keyPass = varchar("key_pass", 50)
    val keyAlias = varchar("key_alias", 50)
    val vkGroupId = integer("vk_group_id")
    val vkToken = varchar("vk_token", 100)
    val vkSecret = varchar("vk_secret", 50)
    val vkConfirmCode = varchar("vk_confirm_code", 10)
    val tgmToken = varchar("tgm_token", 100)
    val photoServerPhrase = varchar("photo_server_phrase", 100)
    val photoLifeTime = integer("photo_life_time")
}

data class Secrets(
    val keyPass: String,
    val keyAlias: String,
    val vkGroupId: Int,
    val vkToken: String,
    val vkSecret: String,
    val vkConfirmCode: String,
    val tgmToken: String,
    val photoServerPhrase: String,
    val photoLifeTime: Int
)
