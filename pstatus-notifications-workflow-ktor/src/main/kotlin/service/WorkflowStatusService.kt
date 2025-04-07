package gov.cdc.ocio.processingnotifications.service

import gov.cdc.ocio.processingnotifications.temporal.WorkflowEngine
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


class WorkflowStatusService : KoinComponent {

    private val workflowEngine by inject<WorkflowEngine>()

    /**
     * Get and return all the Temporal workflows.
     */
    fun getAllWorkflows(): List<Map<String, Any>>  {
        try {
            return workflowEngine.getAllWorkflows().map {
                // Only provide a subset of the workflow status to callers as the other data is superfluous.
                mapOf(
                    "workflowId" to it.workflowId,
                    "taskName" to it.taskName,
                    "description" to it.description,
                    "status" to it.status,
                    "schedule" to it.schedule
                )
            }
        } catch (e: Exception) {
            throw Exception("Error occurred while checking for workflows: ${e.message}")
        }
    }
}
