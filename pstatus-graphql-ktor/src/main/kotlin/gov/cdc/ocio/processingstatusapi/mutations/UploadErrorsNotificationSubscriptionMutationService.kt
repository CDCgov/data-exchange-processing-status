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
 * Upload errors subscription data class which is serialized back and forth which is in turn subscribed in to the MemCache
 * @param dataStreamId String
 * @param dataStreamRoute String
 * @param jurisdiction String
 * @param daysToRun List<String>
 * @param deliveryReference String
 */
@Serializable
data class UploadErrorsNotificationSubscription( val dataStreamId: String,
                                                 val dataStreamRoute: String,
                                                 val jurisdiction: String,
                                                 val daysToRun: List<String>,
                                                 val timeToRun: String,
                                                 val deliveryReference: String)

/**
 * Upload errors unSubscription data class which is serialized back and forth which is in turn used for unsubscribing from the cache for emails and webhooks using the given subscriberId
 * @param subscriptionId
 */
@Serializable
data class UploadErrorsNotificationUnSubscription(val subscriptionId:String)


/**
 * The graphQL mutation service class for upload errors notification subscription/unSubscription
 */

class UploadErrorsNotificationSubscriptionMutationService : Mutation {
    private val uploadErrorsNotificationSubscriptionUrl: String = System.getenv("PSTATUS_WORKFLOW_NOTIFICATIONS_BASE_URL")
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
     *  The mutation function which invokes the upload errors notification microservice route to subscribe to it
     * @param dataStreamId String
     * @param dataStreamRoute String
     * @param jurisdiction String
     * @param daysToRun List<String>
     * @param deliveryReference String
     */

    @GraphQLDescription("Subscribe upload errors lets you get notifications when there are errors in an upload")
    @Suppress("unused")
    fun subscribeUploadErrorsNotification(
        dataStreamId: String,
        dataStreamRoute: String,
        jurisdiction: String,
        daysToRun: List<String>,
        timeToRun: String,
        deliveryReference: String
    ): NotificationSubscriptionResult {
        val url = "$uploadErrorsNotificationSubscriptionUrl/subscribe/uploadErrorsNotification"

        return runBlocking {
            try {
                val response = client.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(
                        UploadErrorsNotificationSubscription(
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
     *  The mutation function which invokes the upload errors in the upload microservice route to unsubscribe
     * @param subscriptionId String
     */

    @GraphQLDescription("UnSubscribe upload errors lets you unsubscribe from getting notifications when there are errors during an upload")
    @Suppress("unused")
    fun unsubscribeUploadErrorsNotification(
        subscriptionId: String
    ): NotificationSubscriptionResult {
        val url = "$uploadErrorsNotificationSubscriptionUrl/unsubscribe/uploadErrorsNotification"

        return runBlocking {
            try {
                val response = client.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(
                        UploadErrorsNotificationUnSubscription(subscriptionId)
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