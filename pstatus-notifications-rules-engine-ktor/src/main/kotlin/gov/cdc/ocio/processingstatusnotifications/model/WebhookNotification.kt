package gov.cdc.ocio.processingstatusnotifications.model

import com.fasterxml.jackson.databind.ObjectMapper
import gov.cdc.ocio.processingstatusnotifications.exception.BadRequestException
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking

data class WebhookNotification(
    private val webhookUrl: String
) : Notification(SubscriptionType.WEBHOOK) {

    /**
     * For webhooks, the payload should be a JSON string.
     *
     * @param payload Any
     */
    override fun doNotify(payload: Any) {
        if (payload !is String || !isJsonValid(payload))
            throw BadRequestException("Webhook payload is not in the expected format")

        runBlocking {
            val client = HttpClient(CIO)

            client.post(webhookUrl) {
                contentType(ContentType.Application.Json)
                setBody(payload)
            }

            client.close()
        }
    }

    private fun isJsonValid(jsonString: String): Boolean {
        return try {
            ObjectMapper().readTree(jsonString)
            true
        } catch (e: Exception) {
            false
        }
    }
}