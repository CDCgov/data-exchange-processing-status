package gov.cdc.ocio.processingstatusapi.mutations

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import gov.cdc.ocio.processingstatusapi.ServiceConnection
import gov.cdc.ocio.processingstatusapi.mutations.response.SubscriptionResponse
import gov.cdc.ocio.types.model.WorkflowSubscriptionDeadlineCheck
import gov.cdc.ocio.types.model.WorkflowSubscriptionResult
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import java.net.ConnectException

/**
 * The graphQL mutation service class for subscribing to and unsubscribing from the upload deadline check.
 */
class DeadlineCheckSubscriptionMutationService(
    workflowServiceUrl: String?
) : Mutation {

    private val workflowServiceConnection =
        ServiceConnection("notifications workflow", workflowServiceUrl)

    /**
     * The mutation function which invokes the upload deadline check microservice route to subscribe.
     *
     * @param subscription WorkflowSubscriptionDeadlineCheck
     * @return WorkflowSubscriptionResult
     */
    @GraphQLDescription("Subscribe Deadline Check lets you get notifications when an upload from jurisdictions has not happened by 12pm")
    @Suppress("unused")
    fun subscribeDeadlineCheck(
        subscription: WorkflowSubscriptionDeadlineCheck
    ): WorkflowSubscriptionResult {
        val url = workflowServiceConnection.buildUrl("/subscribe/deadlineCheck")

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
}