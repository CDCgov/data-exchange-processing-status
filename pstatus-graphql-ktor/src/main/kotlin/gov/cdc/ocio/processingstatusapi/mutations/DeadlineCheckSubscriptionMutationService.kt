@file:Suppress("PLUGIN_IS_NOT_ENABLED")

package gov.cdc.ocio.processingstatusapi.mutations

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable


/**
 * Email Subscription data class which is serialized back and forth which is in turn subscribed in to the MemCache
 * @param dataStreamId String
 * @param dataStreamRoute String
 * @param email String
 * @param stageName String
 * @param statusType String
 */
@Serializable
data class DeadlineCheckSubscription( val dataStreamId: String,
                                      val dataStreamRoute: String,
                                      val jurisdiction: String,
                                      val daysToRun: List<String>,
                                      val timeToRun: String,
                                      val deliveryReference: String)

/**
 * UnSubscription data class which is serialized back and forth which is in turn used for unsubscribing  from the cache for emails and webhooks using the given subscriberId
 * @param subscriptionId
 */
@Serializable
data class DeadlineCheckUnSubscription(val subscriptionId:String)

/**
 * SubscriptionResult is the response class which is serialized back and forth which is in turn used for getting the response which contains the subscriberId , message and the status of subscribe/unsubscribe operations
 * @param subscription_id
 * @param timestamp
 * @param status
 * @param message
 */
@Serializable
data class DeadlineCheckSubscriptionResult(
    var subscriptionId: String? = null,
    var message: String? = "",
    var deliveryReference:String
)

/**
 * The graphQL mutation class for notifications
 */

class DeadlineCheckSubscriptionMutationService : Mutation {
    private val deadlineCheckSubscriptionUrl: String = System.getenv("PSTATUS_NOTIFICATIONS_BASE_URL")
    private val serviceUnavailable =
        "DeadlineCheckSubscription service is unavailable and no connection has been established. Make sure the service is running"
    private val client = HttpClient {
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
     *  SubscribeEmail function which inturn uses the http client to invoke the notifications ktor microservice route to subscribe
     * @param dataStreamId String
     * @param dataStreamRoute String
     * @param email String
     * @param stageName String
     * @param statusType String
     */

    @GraphQLDescription("Subscribe Deadline Check")
    @Suppress("unused")
    fun subscribeDeadlineCheck(
        dataStreamId: String,
        dataStreamRoute: String,
        jurisdiction: String,
        daysToRun: List<String>,
        timeToRun: String,
        deliveryReference: String
    ): DeadlineCheckSubscriptionResult {
        val url = "$deadlineCheckSubscriptionUrl/subscribe/deadlineCheck"

        return runBlocking {
            try {
                val response = client.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(
                        DeadlineCheckSubscription(
                            dataStreamId,
                            dataStreamRoute,
                            jurisdiction,
                            daysToRun,
                            timeToRun,
                            deliveryReference
                        )
                    )
                }
                return@runBlocking ProcessResponse(response)
            } catch (e: Exception) {
                if (e.message!!.contains("Status:")) {
                    ProcessErrorCodes(url, e, null)
                }
                throw Exception(serviceUnavailable)
            }
        }
    }

    /**
     *  SubscribeEmail function which inturn uses the http client to invoke the notifications ktor microservice route to subscribe
     * @param dataStreamId String
     * @param dataStreamRoute String
     * @param email String
     * @param stageName String
     * @param statusType String
     */

    @GraphQLDescription("UnSubscribe Deadline Check")
    @Suppress("unused")
    fun unsubscribeDeadlineCheck(
        subscriptionId: String
    ): DeadlineCheckSubscriptionResult {
        val url = "$deadlineCheckSubscriptionUrl/unsubscribe/deadlineCheck"

        return runBlocking {
            try {
                val response = client.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(
                        DeadlineCheckUnSubscription(subscriptionId)
                    )
                }
                return@runBlocking ProcessResponse(response)
            } catch (e: Exception) {
                if (e.message!!.contains("Status:")) {
                    ProcessErrorCodes(url, e, null)
                }
                throw Exception(serviceUnavailable)
            }
        }
    }



    companion object {
        /**
         * Function to process the http response coming from notifications service
         * @param response HttpResponse
         */
        private suspend fun ProcessResponse(response: HttpResponse): DeadlineCheckSubscriptionResult {
            if (response.status == HttpStatusCode.OK) {
                return response.body()
            } else {
                throw Exception("Notification service is unavailable. Status:${response.status}")
            }
        }
    }


    @Throws(Exception::class)
    /**
     * Function to process the http response codes and throw exception accordingly
     * @param url String
     * @param e Exception
     * @param subscriptionId String?
     */
    internal fun ProcessErrorCodes(url: String, e: Exception, subscriptionId: String?) {
        val error = e.message!!.substringAfter("Status:").substringBefore(" ")
        when (error) {
            "500" -> throw Exception("Subscription with subscriptionId = ${subscriptionId} does not exist in the cache")
            "400" -> throw Exception("Bad Request: Please check the request and retry")
            "401" -> throw Exception("Unauthorized access to notifications service")
            "403" -> throw Exception("Access to notifications service is forbidden")
            "404" -> throw Exception("${url} not found")
            else -> throw Exception(e.message)
        }
    }

}