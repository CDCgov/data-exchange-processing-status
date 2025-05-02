package gov.cdc.ocio.processingstatusapi.mutations

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import gov.cdc.ocio.processingstatusapi.ServiceConnection
import gov.cdc.ocio.processingstatusapi.mutations.response.SubscriptionResponse
import gov.cdc.ocio.types.model.WorkflowSubscriptionForDataStreams
import gov.cdc.ocio.types.model.WorkflowSubscriptionResult
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import java.net.ConnectException


/**
 * DataStream UnSubscription data class used for unsubscribing from the db for digest counts and the top errors and
 * their frequency within an upload.
 *
 * @param subscriptionId
 */
@Serializable
data class DataStreamTopErrorsNotificationUnSubscription(val subscriptionId:String)

/**
 * The graphQL mutation class for dataStream Subscription for digest counts and top5 errors and their frequencies.
 */
class DataStreamTopErrorsNotificationSubscriptionMutationService(
    workflowServiceUrl: String?
) : Mutation {

    private val workflowServiceConnection =
        ServiceConnection("notifications workflow", workflowServiceUrl)

    /**
     * The mutation function which invokes the data stream top errors and digest counts microservice route to subscribe.
     *
     * @param subscription WorkflowSubscriptionForDataStreams
     * @return WorkflowSubscriptionResult
     */
    @GraphQLDescription("Subscribe data stream top errors lets you subscribe to get notifications for top data stream errors and its frequency during an upload")
    @Suppress("unused")
    fun subscribeDataStreamTopErrorsNotification(
        subscription: WorkflowSubscriptionForDataStreams
    ): WorkflowSubscriptionResult {
        val url = workflowServiceConnection.buildUrl("subscribe/dataStreamTopErrorsNotification")

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
     * The mutation function which invokes the data stream top errors and digest counts microservice route to
     * unsubscribe.
     *
     * @param subscriptionId String
    */
    @GraphQLDescription("UnSubscribe data stream top errors lets you unsubscribe from getting notifications for top data stream errors and its frequency during an upload")
    @Suppress("unused")
    fun unsubscribesDataStreamTopErrorsNotification(
        subscriptionId: String
    ): WorkflowSubscriptionResult {
        val url = workflowServiceConnection.buildUrl("/unsubscribe/dataStreamTopErrorsNotification")

        return runBlocking {
            val response = runCatching {
                workflowServiceConnection.client.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(DataStreamTopErrorsNotificationUnSubscription(subscriptionId))
                }
            }.onFailure {
                if (it is ConnectException)
                    throw ConnectException(workflowServiceConnection.serviceUnavailable)
            }.getOrThrow()
            return@runBlocking SubscriptionResponse.ProcessNotificationResponse(response)
        }
    }

}