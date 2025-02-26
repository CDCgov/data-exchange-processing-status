package gov.cdc.ocio.processingnotifications.temporal

import gov.cdc.ocio.processingnotifications.config.TemporalConfig
import gov.cdc.ocio.types.health.HealthCheckResult
import gov.cdc.ocio.types.health.HealthCheckSystem
import gov.cdc.ocio.types.health.HealthStatusType
import io.temporal.api.workflowservice.v1.GetSystemInfoRequest
import io.temporal.serviceclient.WorkflowServiceStubs
import io.temporal.serviceclient.WorkflowServiceStubsOptions


/**
 * Health check implementation of Temporal
 *
 * @param temporalConfig TemporalConfig
 */
class HealthCheckTemporalServer(
    temporalConfig: TemporalConfig
) : HealthCheckSystem("Workflow", "Temporal Server") {

    private val target = temporalConfig.temporalServiceTarget

    /**
     * Checks and sets TemporalHealth status
     */
    override fun doHealthCheck(): HealthCheckResult {
        isTemporalHealthy().onFailure { error ->
            val reason = "Temporal is not healthy: ${error.localizedMessage}"
            logger.error(reason)
            return HealthCheckResult(system, service, HealthStatusType.STATUS_DOWN, reason)
        }
        return HealthCheckResult(system, service, HealthStatusType.STATUS_UP)
    }

    /**
     * Check whether Temporal is healthy.
     *
     * @return Result<Boolean>
     */
    private fun isTemporalHealthy(): Result<Boolean> {
        return try {
            val serviceOptions = WorkflowServiceStubsOptions.newBuilder()
                .setTarget(target)
                .build()

            val serviceStubs = WorkflowServiceStubs.newServiceStubs(serviceOptions)

            val isDown = serviceStubs.isShutdown || serviceStubs.isTerminated
            if (isDown) {
                Result.failure(Exception("Temporal Server is down or terminated"))
            } else {
                serviceStubs.blockingStub()
                    .getSystemInfo(GetSystemInfoRequest.getDefaultInstance()).capabilities
                Result.success(true)
            }
        } catch (ex: Exception) {
            Result.failure(ex)
        }
    }
}
