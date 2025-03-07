package gov.cdc.ocio.processingnotifications.service

import gov.cdc.ocio.processingnotifications.model.WorkflowStatus
import gov.cdc.ocio.processingnotifications.temporal.WorkflowEngine
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


class WorkflowStatusService : KoinComponent {

    private val workflowEngine by inject<WorkflowEngine>()

    /**
     * Get and return all the Temporal workflows.
     */
    fun getAllWorkflows(): List<WorkflowStatus>  {
        try {
            return workflowEngine.getAllWorkflows()
        } catch (e: Exception) {
            throw Exception("Error occurred while checking for workflows: ${e.message}")
        }
    }
}
