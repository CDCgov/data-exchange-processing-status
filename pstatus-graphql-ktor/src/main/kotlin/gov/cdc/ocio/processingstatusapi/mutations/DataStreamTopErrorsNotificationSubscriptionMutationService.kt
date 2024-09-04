package gov.cdc.ocio.processingstatusapi.mutations

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import gov.cdc.ocio.processingstatusapi.mutations.response.SubscriptionResponse
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
data class DataStreamTopErrorsNotificationSubscription( val dataStreamId: String,
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
data class DataStreamTopErrorsNotificationUnSubscription(val subscriptionId:String)

/**
 * SubscriptionResult is the response class which is serialized back and forth which is in turn used for getting the response which contains the subscriberId , message and the status of subscribe/unsubscribe operations
 * @param subscription_id
 * @param timestamp
 * @param status
 * @param message
 */
@Serializable
data class DataStreamTopErrorsNotificationSubscriptionResult(
    var subscriptionId: String? = null,
    var message: String? = "",
    var deliveryReference:String
)

/**
 * The graphQL mutation class for notifications
 */

class DataStreamTopErrorsNotificationSubscriptionMutationService : Mutation {
    private val dataStreamTopErrorsNotificationSubscriptionUrl: String = System.getenv("PSTATUS_NOTIFICATIONS_BASE_URL")
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
    fun subscribeDataStreamTopErrorsNotification(
        dataStreamId: String,
        dataStreamRoute: String,
        jurisdiction: String,
        daysToRun: List<String>,
        timeToRun: String,
        deliveryReference: String
    ): NotificationSubscriptionResult {
        val url = "$dataStreamTopErrorsNotificationSubscriptionUrl/subscribe/dataStreamTopErrorsNotification"

        return runBlocking {
            try {
                val response = client.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(
                        DataStreamTopErrorsNotificationSubscription(
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
     *  SubscribeEmail function which inturn uses the http client to invoke the notifications ktor microservice route to subscribe
     * @param dataStreamId String
     * @param dataStreamRoute String
     * @param email String
     * @param stageName String
     * @param statusType String
     */

    @GraphQLDescription("UnSubscribe Deadline Check")
    @Suppress("unused")
    fun unsubscribesDataStreamTopErrorsNotification(
        subscriptionId: String
    ): NotificationSubscriptionResult {
        val url = "$dataStreamTopErrorsNotificationSubscriptionUrl/unsubscribe/dataStreamTopErrorsNotification"

        return runBlocking {
            try {
                val response = client.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(
                        DataStreamTopErrorsNotificationUnSubscription(subscriptionId)
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