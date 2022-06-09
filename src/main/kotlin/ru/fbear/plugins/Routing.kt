package ru.fbear.plugins

import com.github.kotlintelegrambot.Bot
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import ru.fbear.*

fun Application.configureRouting() {
    val vk = VK()
    val telegram = Telegram().bot
    val photoServer = PhotoServer()

    routing {
        post("/photo_back/vk") {
            val needConfirm = vk.processEvent(call.receiveText())

            if (needConfirm)
                call.respondText(vk.confirmCode)
            else
                call.respondText("ok", status = HttpStatusCode.OK)
        }

        post("/photo_back/tgm") {
            val response = call.receiveText()
            telegram.processUpdate(response)
            call.response.status(HttpStatusCode.OK)
        }

        post("/photo_back/photoserver/photo") {
            val phrase = call.request.queryParameters["phrase"]
            if (phrase == null || phrase != secrets.photoServerPhrase) {
                call.response.status(HttpStatusCode.BadRequest)
                return@post
            }
            photoServer.process(call)
            call.response.status(HttpStatusCode.OK)
        }

        post("/photo_back/photoserver/check") {
            val phrase = call.request.queryParameters["phrase"]
            if (phrase == null || phrase != secrets.photoServerPhrase) {
                call.response.status(HttpStatusCode.BadRequest)
                return@post
            }
            call.response.status(HttpStatusCode.OK)
        }

        get("/photo_back/photoserver/settings") {
            val phrase = call.request.queryParameters["phrase"]
            if (phrase == null || phrase != secrets.photoServerPhrase) {
                call.response.status(HttpStatusCode.BadRequest)
                return@get
            }
            call.respond(BotSettings(secrets.vkGroupId, getTgmUsername(telegram), secrets.photoLifeTime))
        }
    }
}

fun getTgmUsername(telegram: Bot) = telegram.getMe().first?.body()?.result?.username ?: ""
