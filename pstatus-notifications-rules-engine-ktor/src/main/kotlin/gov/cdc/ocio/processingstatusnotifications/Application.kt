package gov.cdc.ocio.processingstatusnotifications

import gov.cdc.ocio.messagesystem.models.MessageSystemType
import gov.cdc.ocio.messagesystem.utils.MessageSystemKoinCreator
import gov.cdc.ocio.messagesystem.utils.createMessageSystemPlugin
import gov.cdc.ocio.processingstatusnotifications.processors.*
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

    routing {
        subscribeEmailNotificationRoute()
        unsubscribeEmailNotificationRoute()
        subscribeWebhookRoute()
        unsubscribeWebhookRoute()
        healthCheckRoute()
        versionRoute()
    }

    createMessageSystemPlugin(MessageSystemType.getFromAppEnv(environment), MessageProcessor())

    install(Koin) {
        loadKoinModules(environment)
    }

    install(ContentNegotiation) {
        jackson()
    }
}
