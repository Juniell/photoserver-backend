package ru.fbear.plugins

import com.github.kotlintelegrambot.Bot
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import ru.fbear.*

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
                    call.response.status(HttpStatusCode.OK)
                }
            }
        }

        post("/photo_back/photoserver/photo") {
            coroutineScope {
                launch {
                    photoServer.process(call)
                    call.response.status(HttpStatusCode.OK)
                }
            }
        }

        post("/photo_back/photoserver/check") {
            coroutineScope {
                launch {
                    call.response.status(HttpStatusCode.OK)
                }
            }
        }

        get("/photo_back/photoserver/settings") {
            coroutineScope {
                launch {
                    call.respond(BotSettings(secrets.vkGroupId, getTgmUsername(telegram), 2)) //todo photoLife
                }
            }
        }
    }
}

fun getTgmUsername(telegram: Bot) = telegram.getMe().first?.body()?.result?.username ?: ""
