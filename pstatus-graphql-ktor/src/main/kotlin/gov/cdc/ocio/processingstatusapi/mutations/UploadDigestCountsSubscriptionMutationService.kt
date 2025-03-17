package gov.cdc.ocio.processingstatusapi.mutations

import gov.cdc.ocio.processingstatusapi.mutations.models.NotificationSubscriptionResult
import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import gov.cdc.ocio.processingstatusapi.ServiceConnection
import gov.cdc.ocio.processingstatusapi.exceptions.ResponseException
import gov.cdc.ocio.processingstatusapi.mutations.response.SubscriptionResponse
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable


/**
 * Daily Upload Digest Counts Subscription data class.
 *
 * @property numDaysAgoToRun Long
 * @property dataStreamIds List<String>
 * @property dataStreamRoutes List<String>
 * @property jurisdictions List<String>
 * @property cronSchedule String
 * @property emailAddresses List<String>
 * @constructor
 */
@Serializable
data class UploadDigestCountsSubscription(
    val numDaysAgoToRun: Long,
    val dataStreamIds: List<String>,
    val dataStreamRoutes: List<String>,
    val jurisdictions: List<String>,
    val cronSchedule: String,
    val emailAddresses: List<String>
)

/**
 * Daily Upload Digest Counts UnSubscription data class which is serialized back and forth which is in turn used for
 * unsubscribing from the cache for emails and webhooks using the given subscriptionId.
 *
 * @param subscriptionId
 */
@Serializable
data class UploadDigestCountsUnSubscription(val subscriptionId: String)

/**
 * The graphQL mutation service class for subscribing to and unsubscribing from a digest of upload counts.
 */
class UploadDigestCountsSubscriptionMutationService(
    workflowServiceUrl: String?
) : Mutation {

    private val workflowServiceConnection =
        ServiceConnection("notifications workflow", workflowServiceUrl)

    /**
     * The mutation function which invokes the daily digest counts microservice route to subscribe.
     *
     * @param dataStreamIds List<String>
     * @param dataStreamRoutes List<String>
     * @param jurisdictions List<String>
     * @param cronSchedule String
     * @param emailAddresses List<String>
     * @return NotificationSubscriptionResult
     */
    @GraphQLDescription("Subscribe daily digest counts lets you get notifications with the counts of all jurisdictions for a given set of data streams after the prescribed time to run is past")
    @Suppress("unused")
    fun subscribeUploadDigestCounts(
        numDaysAgoToRun: Long,
        dataStreamIds: List<String>? = emptyList(),
        dataStreamRoutes: List<String>? = emptyList(),
        jurisdictions: List<String>? = emptyList(),
        cronSchedule: String,
        emailAddresses: List<String>
    ): NotificationSubscriptionResult {
        val url = workflowServiceConnection.getUrl("/subscribe/uploadDigestCounts")

        return runBlocking {
            val result = runCatching {
                val response = workflowServiceConnection.client.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(
                        UploadDigestCountsSubscription(
                            numDaysAgoToRun,
                            dataStreamIds ?: emptyList(),
                            dataStreamRoutes ?: emptyList(),
                            jurisdictions ?: emptyList(),
                            cronSchedule,
                            emailAddresses
                        )
                    )
                }
                return@runCatching SubscriptionResponse.ProcessNotificationResponse(response)
            }
            result.onFailure {
                when (it) {
                    is ResponseException -> throw it
                    else -> throw Exception(workflowServiceConnection.serviceUnavailable)
                }
            }
            return@runBlocking result.getOrThrow()
        }
    }

    /**
     * The mutation function which invokes the daily digest counts microservice route to unsubscribe.
     *
     * @param subscriptionId String
     */
    @GraphQLDescription("UnSubscribe daily digest counts lets you get notifications with the counts of all jurisdictions for a given set of data streams after the prescribed time to run is past")
    @Suppress("unused")
    fun unsubscribeUploadDigestCounts(
        subscriptionId: String
    ): NotificationSubscriptionResult {
        val url = workflowServiceConnection.getUrl("/unsubscribe/uploadDigestCounts")

        return runBlocking {
            try {
                val response = workflowServiceConnection.client.post(url) {
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
                throw Exception(workflowServiceConnection.serviceUnavailable)
            }
        }
    }

}