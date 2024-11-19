package gov.cdc.ocio.processingnotifications

import gov.cdc.ocio.types.health.HealthCheckSystem
import gov.cdc.ocio.types.health.HealthStatusType
import io.temporal.api.workflowservice.v1.GetSystemInfoRequest
import io.temporal.serviceclient.WorkflowServiceStubs
import io.temporal.serviceclient.WorkflowServiceStubsOptions

/**
 * health check implementation of Temporal
 */

class HealthCheckTemporalServer : HealthCheckSystem("Temporal Server") {

    /**
     * Checks and sets TemporalHealth status
     */

    override fun doHealthCheck() {
        try {
        val serviceOptions = WorkflowServiceStubsOptions.newBuilder()
            .setTarget("http://temporaldev-frontend:7233") // Temporal server address //System.getenv().get("")
            .build()
        val serviceStubs = WorkflowServiceStubs.newServiceStubs(serviceOptions)

            val isDown = serviceStubs.isShutdown || serviceStubs.isTerminated
            if (isDown) {
                this.status = HealthStatusType.STATUS_DOWN
                healthIssues = "Temporal Server is down or terminated"
            } else {
                serviceStubs.blockingStub()
                    .getSystemInfo(GetSystemInfoRequest.getDefaultInstance()).capabilities
                this.status = HealthStatusType.STATUS_UP
            }
        } catch (ex: Exception) {
            this.status = HealthStatusType.STATUS_DOWN
            healthIssues = ex.message
            logger.error("Temporal Server is not healthy: ${ex.message}")
        }

    }
}
