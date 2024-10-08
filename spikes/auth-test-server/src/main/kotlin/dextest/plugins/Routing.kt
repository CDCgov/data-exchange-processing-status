package dextest.plugins

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello World!")
        }
        if (authConfig.authEnabled) {
            authenticate("oauth") {
                get("/protected") {
                    call.respondText("You have reached a protected route, ${call.principal<UserIdPrincipal>()?.name}")
                }
            }
        }
        else {
            get("/protected") {
                call.respondText("You have reached a protected route, ${call.principal<UserIdPrincipal>()?.name}")
            }
        }
    }
}
