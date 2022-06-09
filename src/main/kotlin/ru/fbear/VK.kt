package ru.fbear

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.vk.api.sdk.client.TransportClient
import com.vk.api.sdk.client.VkApiClient
import com.vk.api.sdk.client.actors.GroupActor
import com.vk.api.sdk.httpclient.HttpTransportClient
import java.io.File
import kotlin.random.Random


class VK {
    private val groupId = secrets.vkGroupId
    private val accessToken = secrets.vkToken
    private val secret = secrets.vkSecret
    val confirmCode = secrets.vkConfirmCode

    private var transportClient: TransportClient = HttpTransportClient()
    private var vk = VkApiClient(transportClient)
    private var actor = GroupActor(groupId, accessToken)

    /**
     * Обработка пришедших Gson.
     * Возвращает true, если необходимо отправить в ответ код подтверждения (т.е. пришёл confirm).
     **/
    fun processEvent(json: String): Boolean {
        val event: Event = jacksonObjectMapper().readValue(json)

        if (event.secret == secret && event.group_id == groupId) {      // Проверка подлинности
            if (event.type == EventType.CONFIRM.type)
                return true

            if (event.type == EventType.MSG_NEW.type) {
                val msgText = event.`object`!!.message.text

                if (msgText.matches(Regex("""\w*""")))           // Если сообщение похоже на id фото,
                    sendPhoto(event.`object`.message.from_id, msgText)  // пытаемся отправить фото
                else                                                    // Иначе сообщаем, что ждём id фото
                    sendMsg(event.`object`.message.from_id, "Введите идентификатор фото.")
            }
        }
        return false
    }

    /**
     * Отправка сообщения msg пользователю с id = userId.
     **/
    private fun sendMsg(userId: Int, msg: String) {
        vk.messages().send(actor).message(msg).userId(userId).randomId(Random.nextInt()).execute()
    }

    /**
     * Попытка отправки фотографии с названием "$photoId.jpg" пользователю с id = userId.
     * Если фото с таким названием нет, отправляется сообщение, что фото не было найдено.
     **/
    private fun sendPhoto(userId: Int, photoId: String) {
        val file = File(photoDirectory + File.separator + "$photoId.jpg")

        if (!file.exists() || file.isDirectory) {
            sendMsg(userId, "Фото с id = $photoId не было найдено.")
            return
        }

        val uploadUrl = vk.photos().getMessagesUploadServer(actor).execute()
        val uploadResponse = vk.upload().photoMessage(uploadUrl.uploadUrl.toString(), file).execute()
        val photos = vk.photos().saveMessagesPhoto(actor, uploadResponse.photo)
            .server(uploadResponse.server)
            .hash(uploadResponse.hash)
            .execute()
        val photo = photos.first()

        vk.messages().send(actor).userId(userId).attachment("photo${photo.ownerId}_${photo.id}")
            .randomId(Random.nextInt())
            .execute()
    }


    private enum class EventType(val type: String) {
        MSG_NEW("message_new"),
        CONFIRM("confirmation")
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class Event(
        val type: String,
        val group_id: Int,
        val secret: String,
        val `object`: Object?,
        val event_id: String
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class Object(
        val message: Message
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class Message(
        val from_id: Int,
        val text: String
    )
}