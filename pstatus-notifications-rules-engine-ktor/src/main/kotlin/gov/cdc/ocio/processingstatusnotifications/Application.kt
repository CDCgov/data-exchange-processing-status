package gov.cdc.ocio.processingstatusnotifications

import gov.cdc.ocio.database.utils.DatabaseKoinCreator
import gov.cdc.ocio.messagesystem.models.MessageSystemType
import gov.cdc.ocio.messagesystem.utils.MessageSystemKoinCreator
import gov.cdc.ocio.messagesystem.utils.createMessageSystemPlugin
import gov.cdc.ocio.notificationdispatchers.NotificationDispatcherKoinCreator
import gov.cdc.ocio.processingstatusnotifications.processors.*
import gov.cdc.ocio.processingstatusnotifications.subscription.CachedSubscriptionLoader
import gov.cdc.ocio.processingstatusnotifications.subscription.DatabaseSubscriptionLoader
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import org.koin.core.KoinApplication
import org.koin.ktor.plugin.Koin
import org.koin.dsl.module

/**
 * Load the environment configuration values
 *
 * @param environment ApplicationEnvironment
 */
fun KoinApplication.loadKoinModules(
    environment: ApplicationEnvironment
): KoinApplication {
    val databaseModule = DatabaseKoinCreator.moduleFromAppEnv(environment)
    val messageSystemModule = MessageSystemKoinCreator.moduleFromAppEnv(environment)
    val subscriptionLoaderModule = module { single { CachedSubscriptionLoader(DatabaseSubscriptionLoader()) } }
    val notificationDispatcherModule = NotificationDispatcherKoinCreator.moduleFromAppEnv(environment)
    return modules(listOf(databaseModule, messageSystemModule, subscriptionLoaderModule, notificationDispatcherModule))
}

fun main(args: Array<String>) {
    embeddedServer(Netty, commandLineEnvironment(args)).start(wait = true)
}

fun Application.module() {

    routing {
        notificationSubscriptionsRoute()
        notificationSubscriptionRoute()
        subscribeEmailNotificationRoute()
        subscribeWebhookRoute()
        unsubscribeNotificationRoute()
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
