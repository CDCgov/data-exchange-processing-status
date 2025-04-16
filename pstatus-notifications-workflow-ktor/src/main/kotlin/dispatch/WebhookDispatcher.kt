package gov.cdc.ocio.processingnotifications.dispatch

import gov.cdc.ocio.types.model.Notifiable
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking

class WebhookDispatcher(private val url: String) : Dispatcher() {
    override fun dispatch(payload: Notifiable) {
        super.dispatch(payload)
        runBlocking {
            val client = HttpClient(CIO)

            client.post(url) {
                contentType(ContentType.Application.Json)
                setBody(payload.buildWebhookBody())
            }

            client.close()
        }
    }
}