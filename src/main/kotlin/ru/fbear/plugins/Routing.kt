package ru.fbear.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import ru.fbear.VK
import java.io.File

fun Application.configureRouting() {
    val photoDirectory = "photos" + File.separator
    val vk = VK(photoDirectory)

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
    }
}
