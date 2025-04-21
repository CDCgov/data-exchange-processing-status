package gov.cdc.ocio.processingnotifications.webhook

import com.google.gson.GsonBuilder
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking

class WebhookDispatcher {
    fun sendPayload(url: String, body: Any) {
        runBlocking {
            val client = HttpClient(CIO) {
                install(ContentNegotiation) {
                    json()
                }
            }

            client.post(url) {
                contentType(ContentType.Application.Json)
                setBody(GsonBuilder().create().toJson(body))
            }

            client.close()
        }
    }
}