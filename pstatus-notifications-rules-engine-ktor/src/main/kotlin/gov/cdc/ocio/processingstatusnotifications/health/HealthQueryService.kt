package gov.cdc.ocio.processingstatusnotifications.health

import gov.cdc.ocio.database.persistence.ProcessingStatusRepository
import gov.cdc.ocio.messagesystem.MessageSystem
import gov.cdc.ocio.types.health.HealthCheck
import gov.cdc.ocio.types.health.HealthCheckResult
import gov.cdc.ocio.types.health.HealthStatusType
import gov.cdc.ocio.types.utils.TimeUtils
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.system.measureTimeMillis


/**
 * Service for querying the health of the report-sink service and its dependencies.
 *
 * @property logger KLogger
 * @property messageSystem MessageSystem
 */
class HealthQueryService: KoinComponent {

    private val logger = KotlinLogging.logger {}

    private val repository by inject<ProcessingStatusRepository>()

    private val messageSystem by inject<MessageSystem>()

    /**
     * Returns a HealthCheck object with the overall health of the report-sink service and its dependencies.
     *
     * @return HealthCheck
     */
    fun getHealth(): HealthCheck {
        var databaseHealthCheck: HealthCheckResult
        var messageSystemHealthCheck: HealthCheckResult

        val time = measureTimeMillis {
            databaseHealthCheck = repository.healthCheckSystem.doHealthCheck()
            messageSystemHealthCheck = messageSystem.healthCheckSystem.doHealthCheck()
        }

        return HealthCheck().apply {
            status = if (databaseHealthCheck.status == HealthStatusType.STATUS_UP
                && messageSystemHealthCheck.status == HealthStatusType.STATUS_UP
            )
                HealthStatusType.STATUS_UP
            else
                HealthStatusType.STATUS_DOWN

            totalChecksDuration = TimeUtils.formatMillisToHMS(time)
            dependencyHealthChecks.add(databaseHealthCheck)
            dependencyHealthChecks.add(messageSystemHealthCheck)
        }
    }
}