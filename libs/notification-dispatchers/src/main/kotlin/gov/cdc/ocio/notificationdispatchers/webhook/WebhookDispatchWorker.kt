package gov.cdc.ocio.notificationdispatchers.webhook

import gov.cdc.ocio.notificationdispatchers.model.DispatchWorker
import gov.cdc.ocio.notificationdispatchers.model.NotificationContent
import gov.cdc.ocio.notificationdispatchers.model.WebhookNotificationContent
import io.ktor.http.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking


/**
 * Dispatch worker for invoking webhooks.
 */
class WebhookDispatchWorker: DispatchWorker {

    /**
     * Sends a notification with the content provided.
     *
     * @param content NotificationContent
     */
    override fun send(content: NotificationContent) {
        if (content !is WebhookNotificationContent) error("content must be WebhookNotificationContent")

        runBlocking {
            val client = HttpClient(CIO)

            client.post(content.webhookUrl) {
                contentType(ContentType.Application.Json)
                setBody(content)
            }

            client.close()
        }
    }
}