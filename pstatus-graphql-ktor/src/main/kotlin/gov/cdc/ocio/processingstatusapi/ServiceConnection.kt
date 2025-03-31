package gov.cdc.ocio.processingstatusapi

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
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
            json()
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