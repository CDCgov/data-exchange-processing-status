package gov.cdc.ocio.processingstatusnotifications.health

import gov.cdc.ocio.messagesystem.MessageSystem
import gov.cdc.ocio.processingstatusnotifications.subscription.CachedSubscriptionLoader
import gov.cdc.ocio.types.health.HealthCheck
import gov.cdc.ocio.types.health.HealthCheckResult
import gov.cdc.ocio.types.health.HealthStatusType
import gov.cdc.ocio.types.utils.TimeUtils
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.system.measureTimeMillis


/**
 * Service for querying the health of the notifications rules engine service and its dependencies.
 *
 * @property logger KLogger
 * @property messageSystem MessageSystem
 */
class HealthQueryService: KoinComponent {

    private val logger = KotlinLogging.logger {}

    private val subscriptionLoader by inject<CachedSubscriptionLoader>()

    private val messageSystem by inject<MessageSystem>()

    /**
     * Returns a HealthCheck object with the overall health of the notifications rules engine service and its
     * dependencies.
     *
     * @return HealthCheck
     */
    fun getHealth(): HealthCheck {
        var subscriptionLoaderHealthCheck: HealthCheckResult
        var messageSystemHealthCheck: HealthCheckResult

        val time = measureTimeMillis {
            subscriptionLoaderHealthCheck = subscriptionLoader.healthCheckSystem.doHealthCheck()
            messageSystemHealthCheck = messageSystem.healthCheckSystem.doHealthCheck()
        }

        return HealthCheck().apply {
            status = if (subscriptionLoaderHealthCheck.status == HealthStatusType.STATUS_UP
                && messageSystemHealthCheck.status == HealthStatusType.STATUS_UP
            )
                HealthStatusType.STATUS_UP
            else
                HealthStatusType.STATUS_DOWN

            totalChecksDuration = TimeUtils.formatMillisToHMS(time)
            dependencyHealthChecks.add(subscriptionLoaderHealthCheck)
            dependencyHealthChecks.add(messageSystemHealthCheck)
        }
    }
}