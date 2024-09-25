package gov.cdc.ocio.processingstatusapi.mutations

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import gov.cdc.ocio.processingstatusapi.mutations.models.NotificationSubscriptionResult
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
 * DataStream Subscription for digest counts and top5 errors data class which is serialized back and forth which is in turn subscribed in to the MemCache
 * @param dataStreamId String
 * @param dataStreamRoute String
 * @param jurisdiction String
 * @param daysToRun List<String>
 * @param deliveryReference String
 */
@Serializable
data class DataStreamTopErrorsNotificationSubscription( val dataStreamId: String,
                                                        val dataStreamRoute: String,
                                                        val jurisdiction: String,
                                                        val daysToRun: List<String>,
                                                        val timeToRun: String,
                                                        val deliveryReference: String)

/**
 * DataStream UnSubscription data class which is serialized back and forth which is in turn used for unsubscribing  from the db for digest counts and the top errors and their
 * frequency within an upload
 * @param subscriptionId
 */
@Serializable
data class DataStreamTopErrorsNotificationUnSubscription(val subscriptionId:String)

/**
 * The graphQL mutation class for dataStream Subscription for digest counts and top5 errors and their frequencies
 */

class DataStreamTopErrorsNotificationSubscriptionMutationService : Mutation {
    private val dataStreamTopErrorsNotificationSubscriptionUrl: String = System.getenv("PSTATUS_WORKFLOW_NOTIFICATIONS_BASE_URL")
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
     *  The mutation function which invokes the data stream top errors and digest counts microservice route to subscribe
     * @param dataStreamId String
     * @param dataStreamRoute String
     * @param jurisdiction String
     * @param daysToRun List<String>
     * @param deliveryReference String
     */

    @GraphQLDescription("Subscribe data stream top errors lets you subscribe to get notifications for top data stream errors and its frequency during an upload")
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
     *  The mutation function which invokes the data stream top errors and digest counts microservice route to unsubscribe
     * @param subscriptionId String
    */

    @GraphQLDescription("UnSubscribe data stream top errors lets you unsubscribe from getting notifications for top data stream errors and its frequency during an upload")
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