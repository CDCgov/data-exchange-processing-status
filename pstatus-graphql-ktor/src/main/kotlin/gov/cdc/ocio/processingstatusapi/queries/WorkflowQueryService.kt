package gov.cdc.ocio.processingstatusapi.queries

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query
import gov.cdc.ocio.processingstatusapi.ServiceConnection
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking


/**
 * Workflow query service.
 *
 * @property workflowServiceConnection ServiceConnection
 * @constructor
 */
class WorkflowQueryService(
    workflowServiceUrl: String?
) : Query {

    private val workflowServiceConnection =
        ServiceConnection("notifications workflow", workflowServiceUrl)

    @GraphQLDescription("todo")
    @Suppress("unused")
    fun getAllWorkflows(): String {
        val url = workflowServiceConnection.getUrl("/workflows")

        return runBlocking {
            try {
                val response = workflowServiceConnection.client.get(url) {
                    contentType(ContentType.Application.Json)
                }
                response.body()//@runBlocking SubscriptionResponse.ProcessNotificationResponse(response)
            } catch (e: Exception) {
                if (e.message!!.contains("Status:")) {
                    //SubscriptionResponse.ProcessErrorCodes(url, e, null)
                }
                throw Exception(workflowServiceConnection.serviceUnavailable)
            }
        }
    }
}
