package gov.cdc.ocio.processingstatusapi

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.jackson.*
import io.ktor.serialization.kotlinx.json.*


/**
 * A service connection is an HTTP client to a service that provides a REST API.
 *
 * @property serviceUrl String?
 * @property unspecifiedUrl String
 * @property serviceUnavailable String
 * @property client HttpClient
 * @constructor
 */
class ServiceConnection(
    serviceDescription: String,
    private val serviceUrl: String?
) {

    private val unspecifiedUrl =
        "The hostname for the $serviceDescription service has not been provided."

    val serviceUnavailable =
        "The $serviceDescription service is unavailable.  Make sure the $serviceDescription service is running."

    val client = HttpClient {
        install(ContentNegotiation) {
//            json()
            jackson {
                // This is where you customize the ObjectMapper
                registerModule(JavaTimeModule())   // Java time support
                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            }
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.INFO
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 10000
            connectTimeoutMillis = 10000
            socketTimeoutMillis = 10000
        }
    }

    /**
     * Builds a URL with the base service URL and path provided.
     *
     * @param path String
     * @return String
     */
    fun buildUrl(path: String): String {
        if (serviceUrl.isNullOrBlank())
            throw Exception(unspecifiedUrl)

        return "$serviceUrl/$path"
    }
}