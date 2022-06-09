package ru.fbear

import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import kotlinx.serialization.Serializable
import java.io.File

class PhotoServer {
    suspend fun process(call: ApplicationCall) {
        //todo: добавить проверки отправителя
        //todo: добавить проверку на то, что такой файл существует

        val multipartData = call.receiveMultipart()

        multipartData.forEachPart { part ->
            if (part is PartData.FileItem) {
                val fileName = part.originalFileName as String
                val fileBytes = part.streamProvider().readBytes()

                File(photoDirectory + File.separator + fileName).writeBytes(fileBytes)
            }
        }
    }
}
@Serializable
data class BotSettings(
    val vkId: Int,       // id группы в вк
    val tgmId: String,      // id чата телеграмма
    val photoLife: Int      // время хранения фотографий (дни)
)

