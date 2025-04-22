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
import java.time.Instant
import java.util.*


/**
 * Dispatch worker for invoking webhooks.
 */
class WebhookDispatchWorker: DispatchWorker {

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
        if (content !is WebhookNotificationContent) error("content must be WebhookNotificationContent")

        runBlocking {
            val client = HttpClient(CIO) {
                install(ContentNegotiation) {
                    json()
                }
            }

            client.post(content.webhookUrl) {
                contentType(ContentType.Application.Json)
                setBody(gson.toJson(content.payload))
            }

            client.close()
        }
    }

}