package ru.fbear

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import ru.fbear.Info.keyAlias
import ru.fbear.Info.keyPass
import ru.fbear.Info.tgmToken
import ru.fbear.Info.vkConfirmCode
import ru.fbear.Info.vkGroupId
import ru.fbear.Info.vkSecret
import ru.fbear.Info.vkToken
import ru.fbear.plugins.configureRouting
import java.io.File
import java.security.KeyStore
import kotlin.system.exitProcess

val secrets = readDatabase()
val photoDirectory = "photos" + File.separator

fun main() {

    val keyStoreFile = File("keystore.jks")
    val keystore = KeyStore.getInstance(keyStoreFile, secrets.keyPass.toCharArray())

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

        module { configureRouting() }
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
                tgmToken = row[tgmToken]
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

        while (keyPass == null || keyPass == "") {
            println("Enter keyPass:")
            val input = readLine() ?: exitProcess(0)
            keyPass = input
        }
        while (keyAlias == null || keyAlias == "") {
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
        while (vkToken == null || vkToken == "") {
            println("Enter vkToken:")
            val input = readLine() ?: exitProcess(0)
            vkToken = input
        }
        while (vkSecret == null || vkSecret == "") {
            println("Enter vkSecret:")
            val input = readLine() ?: exitProcess(0)
            vkSecret = input
        }
        while (vkConfirmCode == null || vkConfirmCode == "") {
            println("Enter vkConfirmCode:")
            val input = readLine() ?: exitProcess(0)
            vkConfirmCode = input
        }
        while (tgmToken == null || tgmToken == "") {
            println("Enter tgmToken:")
            val input = readLine() ?: exitProcess(0)
            tgmToken = input
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
            }
            commit()
        }

        Secrets(keyPass, keyAlias, vkGroupId, vkToken, vkSecret, vkConfirmCode, tgmToken)
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
}

data class Secrets(
    val keyPass: String,
    val keyAlias: String,
    val vkGroupId: Int,
    val vkToken: String,
    val vkSecret: String,
    val vkConfirmCode: String,
    val tgmToken: String
)
