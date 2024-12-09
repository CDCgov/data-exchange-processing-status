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

    var status = HealthStatusType.STATUS_DOWN
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

        val temporalHealthCheck = HealthCheckTemporalServer(temporalConfig)
        val databaseHealthCheck: HealthCheckSystem?

        val time = measureTimeMillis {
            databaseHealthCheck = when (databaseType) {
                DatabaseType.COSMOS -> getKoin().get<HealthCheckCosmosDb>()
                DatabaseType.MONGO -> getKoin().get<HealthCheckMongoDb>()
                DatabaseType.COUCHBASE -> getKoin().get<HealthCheckCouchbaseDb>()
                DatabaseType.DYNAMO -> getKoin().get<HealthCheckDynamoDb>()
                else -> HealthCheckUnsupportedDb()
            }
            databaseHealthCheck.doHealthCheck()
            temporalHealthCheck.doHealthCheck()
        }


        return HealthCheck().apply {
            status = if (databaseHealthCheck?.status == HealthStatusType.STATUS_UP
                && temporalHealthCheck.status == HealthStatusType.STATUS_UP
            )
                HealthStatusType.STATUS_UP else HealthStatusType.STATUS_DOWN


             totalChecksDuration = formatMillisToHMS(time)
             databaseHealthCheck?.let {dependencyHealthChecks.add(it)}
             temporalHealthCheck.let {dependencyHealthChecks.add(it)}

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