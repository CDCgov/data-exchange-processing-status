package gov.cdc.ocio.processingnotifications.service

import gov.cdc.ocio.processingnotifications.temporal.WorkflowEngine
import io.temporal.api.workflow.v1.WorkflowExecutionInfo
import mu.KotlinLogging


class WorkflowStatusService {

    private val logger = KotlinLogging.logger {}

    private val workflowEngine = WorkflowEngine()

    /**
     * Get and return all the Temporal workflows.
     */
    fun getAllWorkflows(): List<WorkflowExecutionInfo>  {
        try {
            return workflowEngine.getAllWorkflows()
        } catch (e: Exception) {
            logger.error("Error occurred while checking for workflows: ${e.message}")
        }
        throw Exception("Error occurred while checking for workflows")
    }
}
