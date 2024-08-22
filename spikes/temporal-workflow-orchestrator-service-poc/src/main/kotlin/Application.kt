package gov.cdc.ocio.processingnotifications

import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*

fun main(args: Array<String>) {
    embeddedServer(Netty, commandLineEnvironment(args)).start(wait = true)
}

fun Application.module() {

    install(ContentNegotiation) {
        jackson()
    }
    routing {
        subscribeDeadlineCheckRoute()
    }

}
