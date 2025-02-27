package gov.cdc.ocio.processingnotifications.service

import gov.cdc.ocio.processingnotifications.model.WorkflowStatus
import gov.cdc.ocio.processingnotifications.temporal.WorkflowEngine
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


class WorkflowStatusService : KoinComponent {

    private val logger = KotlinLogging.logger {}

    private val workflowEngine by inject<WorkflowEngine>()

    /**
     * Get and return all the Temporal workflows.
     */
    fun getAllWorkflows(): List<WorkflowStatus>  {
        try {
            return workflowEngine.getAllWorkflows()
        } catch (e: Exception) {
            logger.error("Error occurred while checking for workflows: ${e.message}")
        }
        throw Exception("Error occurred while checking for workflows")
    }
}
