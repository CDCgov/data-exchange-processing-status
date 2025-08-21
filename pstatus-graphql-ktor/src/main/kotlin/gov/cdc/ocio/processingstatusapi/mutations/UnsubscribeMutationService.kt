package gov.cdc.ocio.processingstatusapi.mutations

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import gov.cdc.ocio.processingstatusapi.ServiceConnection
import gov.cdc.ocio.processingstatusapi.mutations.response.SubscriptionResponse
import gov.cdc.ocio.types.model.WorkflowSubscriptionResult
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import java.net.ConnectException

class UnsubscribeMutationService(workflowServiceUrl: String?) : Mutation {
    private val workflowServiceConnection =
        ServiceConnection("notifications workflow", workflowServiceUrl)

    /**
     * The mutation function which invokes the unsubscribe microservice route to
     * unsubscribe.
     *
     * @param subscriptionId String
     */
    @GraphQLDescription("Unsubscribe lets you unsubscribe from getting notifications for a given workflow")
    @Suppress("unused")
    fun unsubscribeNotificationWorkflow(
        subscriptionId: String
    ): WorkflowSubscriptionResult {
        val url = workflowServiceConnection.buildUrl("/unsubscribe")

        return runBlocking {
            val response = runCatching {
                workflowServiceConnection.client.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(UnsubscribeRequest(subscriptionId))
                }
            }.onFailure {
                if (it is ConnectException)
                    throw ConnectException(workflowServiceConnection.serviceUnavailable)
            }.getOrThrow()
            return@runBlocking SubscriptionResponse.ProcessNotificationResponse(response)
        }
    }
}