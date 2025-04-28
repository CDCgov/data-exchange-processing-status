package gov.cdc.ocio.processingstatusapi.mutations

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import gov.cdc.ocio.processingstatusapi.ServiceConnection
import gov.cdc.ocio.processingstatusapi.mutations.response.SubscriptionResponse
import gov.cdc.ocio.types.model.WorkflowSubscription
import gov.cdc.ocio.types.model.WorkflowSubscriptionForDataStreams
import gov.cdc.ocio.types.model.WorkflowSubscriptionResult
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import java.net.ConnectException


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
     * @param subscription WorkflowSubscription
     * @return WorkflowSubscriptionResult
     */
    @GraphQLDescription("Subscribe daily digest counts lets you get notifications with the counts of all jurisdictions for a given set of data streams after the prescribed time to run is past")
    @Suppress("unused")
    fun subscribeUploadDigestCounts(
        subscription: WorkflowSubscriptionForDataStreams
    ): WorkflowSubscriptionResult {
        val url = workflowServiceConnection.buildUrl("/subscribe/uploadDigestCounts")

        return runBlocking {
            val response = runCatching {
                workflowServiceConnection.client.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(subscription)
                }
            }.onFailure {
                if (it is ConnectException)
                    throw ConnectException(workflowServiceConnection.serviceUnavailable)
            }.getOrThrow()
            return@runBlocking SubscriptionResponse.ProcessNotificationResponse(response)
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
    ): WorkflowSubscriptionResult {
        val url = workflowServiceConnection.buildUrl("/unsubscribe/uploadDigestCounts")

        return runBlocking {
            val response = runCatching {
                workflowServiceConnection.client.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(UploadDigestCountsUnSubscription(subscriptionId))
                }
            }.onFailure {
                if (it is ConnectException)
                    throw ConnectException(workflowServiceConnection.serviceUnavailable)
            }.getOrThrow()
            return@runBlocking SubscriptionResponse.ProcessNotificationResponse(response)
        }
    }

}