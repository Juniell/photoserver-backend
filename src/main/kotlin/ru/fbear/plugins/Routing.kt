package ru.fbear.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import ru.fbear.PhotoServer
import ru.fbear.Telegram
import ru.fbear.VK

fun Application.configureRouting() {
    val vk = VK()
    val telegram = Telegram().bot
    val photoServer = PhotoServer()

    routing {
        post("/photo_back/vk") {
            coroutineScope {
                launch {
                    val needConfirm = vk.processEvent(call.receiveText())

                    if (needConfirm)
                        call.respondText(vk.confirmCode)
                    else
                        call.respondText("ok", status = HttpStatusCode.OK)
                }
            }
        }

        post("/photo_back/tgm") {
            coroutineScope {
                launch {
                    val response = call.receiveText()
                    telegram.processUpdate(response)
                    call.respond(HttpStatusCode.OK)
                }
            }
        }

        post("/photo_back/photoserver/photo") {
            coroutineScope {
                launch {
                    photoServer.process(call)
                }
            }
        }
    }
}
