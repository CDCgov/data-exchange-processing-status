package dextest

import dextest.plugins.configureAuth
import dextest.plugins.configureRouting
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*

fun main() {
    System.setProperty("io.ktor.development", "true")
    embeddedServer(
        Netty, watchPaths = listOf("classes", "resources"), port = 8001, host = "0.0.0.0", module = Application::module
    ).start(wait = true)
}

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json()
    }
}

fun Application.module() {
    configureSerialization()
    configureAuth()
    configureRouting()
}
