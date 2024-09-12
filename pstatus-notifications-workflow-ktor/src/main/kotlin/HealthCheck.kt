package gov.cdc.ocio.processingnotifications

import io.temporal.api.workflowservice.v1.GetSystemInfoRequest
import io.temporal.serviceclient.WorkflowServiceStubs
import io.temporal.serviceclient.WorkflowServiceStubsOptions
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import kotlin.system.measureTimeMillis

/**
 * Abstract class used for modeling the health issues of an individual service.
 *
 * @property status String
 * @property healthIssues String?
 * @property service String
 */
abstract class HealthCheckSystem {

    var status: String = "DOWN"
    var healthIssues: String? = ""
    open val service: String = ""
}

/**
 * Concrete implementation of the Temporal Server health check.
 *
 * @property service String
 */
class HealthCheckTemporalServer: HealthCheckSystem() {
    override val service: String = "Temporal Server"
}
/**
 * Run health checks for the service.
 *
 * @property status String?
 * @property totalChecksDuration String?
 * @property dependencyHealthChecks MutableList<HealthCheckSystem>
 */
class HealthCheck {

    var status: String = "DOWN"
    var totalChecksDuration: String? = null
    var dependencyHealthChecks = mutableListOf<HealthCheckSystem>()
}

/**
 * Service for querying the health of the temporal server and its dependencies.
 *
 * @property logger KLogger

 */
class TemporalHealthCheckService: KoinComponent {
    private val logger = KotlinLogging.logger {}
    private val serviceOptions = WorkflowServiceStubsOptions.newBuilder()
        .setTarget(System.getenv().get("")) // Temporal server address
        .build()
    private val serviceStubs = WorkflowServiceStubs.newServiceStubs(serviceOptions)

    /**
     * Returns a HealthCheck object with the overall health of temporal server and its dependencies.
     *
     * @return HealthCheck
     */
    fun getHealth(): HealthCheck {
        val temporalHealth = HealthCheckTemporalServer()
        val time = measureTimeMillis {
            try {
                val isDown= serviceStubs.isShutdown  || serviceStubs.isTerminated
                if(isDown)
                {
                    temporalHealth.status ="DOWN"
                    temporalHealth.healthIssues= "Temporal Server is down or terminated"
                }
                else {
                    // serviceStubs.healthCheck() - issue finding the proper version for grpc-health-check
                    // Simple call to get the server capabilities to test if it's up
                    serviceStubs.blockingStub()
                        .getSystemInfo(GetSystemInfoRequest.getDefaultInstance()).capabilities
                    temporalHealth.status = "UP"
                }
            } catch (ex: Exception) {
                temporalHealth.status ="DOWN"
                temporalHealth.healthIssues= ex.message
                logger.error("Temporal Server is not healthy: ${ex.message}")
            }
        }

        return HealthCheck().apply {
            status = temporalHealth.status
            totalChecksDuration = formatMillisToHMS(time)
            dependencyHealthChecks.add(temporalHealth)
        }
    }

    /**
     * Format the time in milliseconds to 00:00:00.000 format.
     *
     * @param millis Long
     * @return String
     */
    private fun formatMillisToHMS(millis: Long): String {
        val seconds = millis / 1000
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val remainingSeconds = seconds % 60
        val remainingMillis = millis % 1000

        return "%02d:%02d:%02d.%03d".format(hours, minutes, remainingSeconds, remainingMillis / 10)
    }
}