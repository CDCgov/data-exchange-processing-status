package gov.cdc.ocio.processingstatusapi.queries

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query
import gov.cdc.ocio.processingstatusapi.ServiceConnection
import gov.cdc.ocio.processingstatusapi.models.query.WorkflowStatus
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

    @GraphQLDescription("A workflow describes the type of notification and schedule for evaluating if a notification is sent.")
    @Suppress("unused")
    fun getAllWorkflows(): List<WorkflowStatus> {
        val url = workflowServiceConnection.getUrl("/workflows")

        return runBlocking {
            try {
                val response = workflowServiceConnection.client.get(url) {
                    contentType(ContentType.Application.Json)
                }
                if (response.status == HttpStatusCode.OK) {
                    return@runBlocking response.body()
                } else {
                    throw Exception("Service unavailable. Status: ${response.status}")
                }
            } catch (e: Exception) {
                throw Exception(workflowServiceConnection.serviceUnavailable)
            }
        }
    }
}
