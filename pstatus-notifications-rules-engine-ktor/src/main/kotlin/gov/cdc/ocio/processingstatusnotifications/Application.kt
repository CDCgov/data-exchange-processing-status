package gov.cdc.ocio.processingstatusnotifications

import gov.cdc.ocio.messagesystem.utils.MessageSystemKoinCreator
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import org.koin.core.KoinApplication
import org.koin.ktor.plugin.Koin


/**
 * Load the environment configuration values
 *
 * @param environment ApplicationEnvironment
 */
fun KoinApplication.loadKoinModules(
    environment: ApplicationEnvironment
): KoinApplication {
    val messageSystemModule = MessageSystemKoinCreator.moduleFromAppEnv(environment)
    return modules(listOf(messageSystemModule))
}

fun main(args: Array<String>) {
    embeddedServer(Netty, commandLineEnvironment(args)).start(wait = true)
}

fun Application.module() {
    install(Koin) {
        loadKoinModules(environment)
    }

    install(ContentNegotiation) {
        jackson()
    }

    routing {
        subscribeEmailNotificationRoute()
        unsubscribeEmailNotificationRoute()
        subscribeWebhookRoute()
        unsubscribeWebhookRoute()
        healthCheckRoute()
        versionRoute()
    }
}
