package gov.cdc.ocio.processingnotifications


import gov.cdc.ocio.database.DatabaseType

import gov.cdc.ocio.database.health.HealthCheckCouchbaseDb
import gov.cdc.ocio.database.health.HealthCheckDynamoDb

import gov.cdc.ocio.database.health.HealthCheckUnsupportedDb
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.system.measureTimeMillis
import gov.cdc.ocio.database.health.*
import gov.cdc.ocio.processingnotifications.config.TemporalConfig
import gov.cdc.ocio.types.health.HealthCheckSystem
import gov.cdc.ocio.types.health.HealthStatusType


/**
 * Abstract class used for modeling the health issues of an individual service.
 *
 * @property status String
 * @property healthIssues String?
 * @property service String
 */
class HealthCheckSystem {

    var status: String = "DOWN"
    var healthIssues: String? = ""
    open var service: String = ""
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

    //val service = "Cosmos DB"
    var dependencyHealthChecks = mutableListOf<HealthCheckSystem>()
}

/**
 * Service for querying the health of the temporal server and its dependencies.
 *
 * @property logger KLogger

 */
class HealthCheckService : KoinComponent {
    private val logger = KotlinLogging.logger {}
    private val databaseType: DatabaseType by inject()
    private val temporalConfig: TemporalConfig by inject()

    /**
     * Returns a HealthCheck object with the overall health of temporal server and its dependencies.
     *
     * @return HealthCheck
     */
    fun getHealth(): HealthCheck {

        val temporalHealth = HealthCheckTemporalServer(temporalConfig)
        val databaseHealthCheck: HealthCheckSystem?

        val time = measureTimeMillis {
            databaseHealthCheck = when (databaseType) {
                DatabaseType.COSMOS -> HealthCheckCosmosDb()
                DatabaseType.DYNAMO -> HealthCheckDynamoDb()
                DatabaseType.COUCHBASE -> HealthCheckCouchbaseDb()
                else -> HealthCheckUnsupportedDb()
            }
            databaseHealthCheck.doHealthCheck()
        }


        return HealthCheck().apply {
            status = temporalHealth.doHealthCheck().toString()

            status = if (databaseHealthCheck?.status == HealthStatusType.STATUS_UP)
                databaseType.toString() + " is " + HealthStatusType.STATUS_UP.value
            else
                HealthStatusType.STATUS_DOWN.value
            totalChecksDuration = formatMillisToHMS(time)
            dependencyHealthChecks.add(temporalHealth)
        }
    }

    /**
     * Format the time in milliseconds to 00:00:00.000 format.
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