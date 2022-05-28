package ru.fbear

import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.text
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.TelegramFile
import com.github.kotlintelegrambot.webhook
import java.io.File

class Telegram {
    private val startMsg = "Привет, я бот для получения фотографий с мероприятий. " +
            "Отправьте мне id фото, которое вас интересует, и я немедленно отправлю его вам."

    val bot = bot {
        token = secrets.tgmToken

        webhook {
            url = "https://bot.fbear.ru/photo_back/tgm"
            allowedUpdates = listOf("message")
        }

        dispatch {
            command("start") {
                bot.sendMessage(ChatId.fromId(message.chat.id), startMsg)

                val idPhoto = message.text?.removePrefix("/start ") ?: ""
                if (idPhoto.isNotEmpty() && idPhoto != "/start") {
                    bot.sendMessage(ChatId.fromId(message.chat.id), "Был получен id = $idPhoto")
                    sendPhoto(ChatId.fromId(message.chat.id), idPhoto)
                }
            }

            text {
                val text = message.text
                if (text != null && text.matches(Regex("""\w*""")))
                    sendPhoto(ChatId.fromId(message.chat.id), text)
                else
                    bot.sendMessage(ChatId.fromId(message.chat.id), "Введите идентификатор фото.")
            }
        }
    }.apply { startWebhook() }

    /**
     * Попытка отправки фотографии с названием "$photoId.jpg" в чат с id = chatId.
     * Если фото с таким названием нет, отправляется сообщение, что фото не было найдено.
     **/
    private fun sendPhoto(chatId: ChatId.Id, photoId: String) {
        val file = File("$photoDirectory$photoId.jpg")

        if (!file.exists() || file.isDirectory) {
            bot.sendMessage(chatId, "Фото с id = $photoId не было найдено.")
            return
        }
        val photo = TelegramFile.ByFile(file)
        bot.sendPhoto(chatId, photo)
    }
}