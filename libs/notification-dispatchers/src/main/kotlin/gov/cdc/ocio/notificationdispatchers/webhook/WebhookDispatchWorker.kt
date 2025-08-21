package gov.cdc.ocio.notificationdispatchers.webhook

import com.google.gson.GsonBuilder
import com.google.gson.ToNumberPolicy
import gov.cdc.ocio.notificationdispatchers.model.DispatchWorker
import gov.cdc.ocio.notificationdispatchers.model.NotificationContent
import gov.cdc.ocio.notificationdispatchers.model.WebhookNotificationContent
import gov.cdc.ocio.types.adapters.DateLongFormatTypeAdapter
import gov.cdc.ocio.types.adapters.InstantTypeAdapter
import io.ktor.http.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import java.io.EOFException
import java.time.Instant
import java.util.*


/**
 * Dispatch worker for invoking webhooks.
 */
class WebhookDispatchWorker: DispatchWorker {

    private val logger = KotlinLogging.logger {}

    private val gson =
        GsonBuilder()
            .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
            .registerTypeAdapter(Date::class.java, DateLongFormatTypeAdapter())
            .registerTypeAdapter(Instant::class.java, InstantTypeAdapter())
            .create()

    /**
     * Sends a notification with the content provided.
     *
     * @param content NotificationContent
     */
    override fun send(content: NotificationContent) {
        if (content !is WebhookNotificationContent)
            throw IllegalArgumentException("content must be WebhookNotificationContent")

        runBlocking {
            val client = HttpClient(CIO) {
                // Timeout configurations
                engine {
                    requestTimeout = 10000 // 10 seconds
                }
                install(ContentNegotiation) {
                    json()
                }
            }

            try {
                val response = client.post(content.webhookUrl) {
                    contentType(ContentType.Application.Json)
                    setBody(gson.toJson(content.payload))
                }

                // Handle HTTP response
                when (response.status) {
                    HttpStatusCode.OK -> logger.info("Webhook sent successfully!")
                    else -> logger.error("Unexpected response: ${response.status}")
                }

            } catch (e: EOFException) {
                logger.error("EOFException occurred: ${e.message}. Possible server disconnection issue.")
            } catch (e: Exception) {
                logger.error("An error occurred during the HTTP request: ${e.message}")
            } finally {
                client.close()
            }
        }
    }

}