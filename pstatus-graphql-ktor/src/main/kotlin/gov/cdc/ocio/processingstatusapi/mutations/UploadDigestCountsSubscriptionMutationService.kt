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
 * Daily Upload Digest Counts Subscription data class which is serialized back and forth which is in turn subscribed in to the MemCache
 * @param jurisdictionIds List<String>
 * @param dataStreamIds List<String>
 * @param daysToRun List<String>
 * @param timeToRun String
 * @param deliveryReference String
 */
@Serializable
data class UploadDigestCountsSubscription(val jurisdictionIds: List<String>,
                                                         val dataStreamIds: List<String>,
                                                         val daysToRun: List<String>,
                                                         val timeToRun: String,
                                                         val deliveryReference: String)

/**
 * Daily Upload Digest Counts UnSubscription data class which is serialized back and forth which is in turn used for unsubscribing from the cache for emails and webhooks using the given subscriberId
 * @param subscriptionId
 */
@Serializable
data class UploadDigestCountsUnSubscription(val subscriptionId:String)

/**
 * The graphQL mutation service class for Daily Upload Digest Counts Subscription/UnSubscription
 */

class UploadDigestCountsSubscriptionMutationService : Mutation {
    private val uploadDigestCountsSubscriptionUrl: String = System.getenv("PSTATUS_WORKFLOW_NOTIFICATIONS_BASE_URL")
    private val serviceUnavailable =
        "UploadDigestCountsSubscription service is unavailable and no connection has been established. Make sure the service is running"
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
     *   The mutation function which invokes the daily digest counts microservice route to subscribe
     * @param jurisdictionIds List<String>
     * @param dataStreamIds List<String>
     * @param daysToRun List<String>
     * @param timeToRun String
     * @param deliveryReference String
     */

    @GraphQLDescription("Subscribe daily digest counts lets you get notifications with the counts of all jurisdictions for a given set of data streams after the prescribed time to run is past")
    @Suppress("unused")
    fun subscribeUploadDigestCounts(
         jurisdictionIds: List<String>,
         dataStreamIds: List<String>,
         daysToRun: List<String>,
         timeToRun: String,
         deliveryReference: String
    ): NotificationSubscriptionResult {
        val url = "$uploadDigestCountsSubscriptionUrl/subscribe/uploadDigestCounts"

        return runBlocking {
            try {
                val response = client.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(
                        UploadDigestCountsSubscription(
                            jurisdictionIds,
                            dataStreamIds,
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
     *  The mutation function which invokes the daily digest counts microservice route to unsubscribe
     * @param subscriptionId String
     */

    @GraphQLDescription("UnSubscribe daily digest counts lets you get notifications with the counts of all jurisdictions for a given set of data streams after the prescribed time to run is past")
    @Suppress("unused")
    fun unsubscribeUploadDigestCounts(
        subscriptionId: String
    ): NotificationSubscriptionResult {
        val url = "$uploadDigestCountsSubscriptionUrl/unsubscribe/uploadDigestCounts"

        return runBlocking {
            try {
                val response = client.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(
                        UploadDigestCountsUnSubscription(subscriptionId)
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