package gov.cdc.ocio.processingstatusnotifications.model

import gov.cdc.ocio.processingstatusnotifications.exception.BadRequestException
import gov.cdc.ocio.types.model.WebhookNotification
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking


class WebhookNotificationAction(
    private val webhookNotification: WebhookNotification
) : NotificationAction {

    /**
     * For webhooks, the payload should be [WebhookContent].
     *
     * @param payload Any
     */
    override fun doNotify(payload: Any) {
        if (payload !is WebhookContent)
            throw BadRequestException("Webhook payload is not in the expected format")

        runBlocking {
            val client = HttpClient(CIO)

            client.post(webhookNotification.webhookUrl) {
                contentType(ContentType.Application.Json)
                setBody(payload.toJson())
            }

            client.close()
        }
    }
}
