package gov.cdc.ocio.processingstatusapi.mutations

import gov.cdc.ocio.processingstatusapi.mutations.models.NotificationSubscriptionResult
import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import gov.cdc.ocio.processingstatusapi.mutations.response.SubscriptionResponse
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable


/**
 * Deadline Check Subscription data class which is serialized back and forth which is in turn subscribed in to the MemCache
 * @param dataStreamId String
 * @param dataStreamRoute String
 * @param jurisdiction String
 * @param daysToRun List<String>
 * @param deliveryReference String
 */
@Serializable
data class DeadlineCheckSubscription( val dataStreamId: String,
                                      val dataStreamRoute: String,
                                      val jurisdiction: String,
                                      val daysToRun: List<String>,
                                      val timeToRun: String,
                                      val deliveryReference: String)

/**
 * Deadline check unSubscription data class which is serialized back and forth which is in turn used for unsubscribing from the cache for emails and webhooks using the given subscriberId
 * @param subscriptionId
 */
@Serializable
data class DeadlineCheckUnSubscription(val subscriptionId:String)

/**
 * The graphQL mutation service class for deadline check subscription/unSubscription
 */

class DeadlineCheckSubscriptionMutationService : Mutation {
    private val deadlineCheckSubscriptionUrl: String = System.getenv("PSTATUS_WORKFLOW_NOTIFICATIONS_BASE_URL")
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
     *  The mutation function which invokes the upload deadline check microservice route to subscribe
     * @param dataStreamId String
     * @param dataStreamRoute String
     * @param jurisdiction String
     * @param daysToRun List<String>
     * @param deliveryReference String
     */

    @GraphQLDescription("Subscribe Deadline Check which lets you subscribe to notifications if upload from jurisdictions doesn't occur within a stipulated time")
    @Suppress("unused")
    fun subscribeDeadlineCheck(
        dataStreamId: String,
        dataStreamRoute: String,
        jurisdiction: String,
        daysToRun: List<String>,
        timeToRun: String,
        deliveryReference: String
    ): NotificationSubscriptionResult {
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
                return@runBlocking SubscriptionResponse.ProcessNotificationResponse(response)
            } catch (e: Exception) {
                if (e.message!!.contains("Status:")) {
                    SubscriptionResponse.ProcessErrorCodes(url, e, null)
                }
                throw Exception(serviceUnavailable)
            }
        }
    }

    /**
     *  The mutation function which invokes the upload deadline check microservice route to unsubscribe
     * @param subscriptionId String
     */

    @GraphQLDescription("UnSubscribe Deadline Check which lets you unsubscribe from getting notifications if upload from jurisdictions doesn't occur within a stipulated time")
    @Suppress("unused")
    fun unsubscribeDeadlineCheck(
        subscriptionId: String
    ): NotificationSubscriptionResult {
        val url = "$deadlineCheckSubscriptionUrl/unsubscribe/deadlineCheck"

        return runBlocking {
            try {
                val response = client.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(
                        DeadlineCheckUnSubscription(subscriptionId)
                    )
                }
                return@runBlocking SubscriptionResponse.ProcessNotificationResponse(response)
            } catch (e: Exception) {
                if (e.message!!.contains("Status:")) {
                    SubscriptionResponse.ProcessErrorCodes(url, e, null)
                }
                throw Exception(serviceUnavailable)
            }
        }
    }

}