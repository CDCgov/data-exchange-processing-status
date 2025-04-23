package gov.cdc.ocio.notificationdispatchers

import gov.cdc.ocio.notificationdispatchers.email.EmailDispatchWorker
import gov.cdc.ocio.notificationdispatchers.logger.LoggerDispatchWorker
import gov.cdc.ocio.notificationdispatchers.model.DispatchWorker
import gov.cdc.ocio.notificationdispatchers.model.EmailNotificationContent
import gov.cdc.ocio.notificationdispatchers.model.NotificationContent
import gov.cdc.ocio.notificationdispatchers.model.WebhookNotificationContent
import gov.cdc.ocio.notificationdispatchers.webhook.WebhookDispatchWorker
import io.ktor.server.application.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mu.KotlinLogging


class NotificationDispatcher(environment: ApplicationEnvironment): DispatchWorker {

    private val logger = KotlinLogging.logger {}

    private val emailDispatcher = EmailDispatchWorker.create(environment)
    private val webhookDispatcher = WebhookDispatchWorker()
    private val loggingDispatcher = LoggerDispatchWorker()

    /**
     * Sends a notification with the content provided.
     *
     * @param content NotificationContent
     */
    override fun send(content: NotificationContent) {
        CoroutineScope(Dispatchers.Default).launch {
            loggingDispatcher.send(content) // log all notifications
            when (content) {
                is EmailNotificationContent -> emailDispatcher.send(content)
                is WebhookNotificationContent -> webhookDispatcher.send(content)
                else -> logger.warn { "Notification not sent, only logged." }
            }
        }
    }
}