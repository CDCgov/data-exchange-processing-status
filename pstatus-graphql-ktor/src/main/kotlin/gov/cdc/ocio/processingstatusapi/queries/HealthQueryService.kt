package gov.cdc.ocio.processingstatusapi.queries

import com.expediagroup.graphql.server.operations.Query
import gov.cdc.ocio.database.persistence.ProcessingStatusRepository
import gov.cdc.ocio.processingstatusapi.models.graphql.GraphQLHealthCheck
import gov.cdc.ocio.processingstatusapi.models.graphql.GraphQLHealthCheckResult
import gov.cdc.ocio.reportschemavalidator.loaders.SchemaLoader
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
 * @property repository ProcessingStatusRepository
 * @property schemaLoader SchemaLoader
 */
class HealthCheckService: KoinComponent {

    private val logger = KotlinLogging.logger {}

    private val repository by inject<ProcessingStatusRepository>()

    private val schemaLoader by inject<SchemaLoader>()

    /**
     * Returns a HealthCheck object with the overall health of the report-sink service and its dependencies.
     *
     * @return HealthCheck
     */
    fun getHealth(): GraphQLHealthCheck {
        val databaseHealthCheck: HealthCheckResult
        val schemaLoaderSystemHealthCheck: HealthCheckResult

        val time = measureTimeMillis {
            databaseHealthCheck = repository.healthCheckSystem.doHealthCheck()
            schemaLoaderSystemHealthCheck = schemaLoader.healthCheckSystem.doHealthCheck()
        }

        return GraphQLHealthCheck().apply {
            status = if (databaseHealthCheck.status == HealthStatusType.STATUS_UP
                && schemaLoaderSystemHealthCheck.status == HealthStatusType.STATUS_UP
            )
                HealthStatusType.STATUS_UP.value
            else
                HealthStatusType.STATUS_DOWN.value
            totalChecksDuration = TimeUtils.formatMillisToHMS(time)
            dependencyHealthChecks.add(GraphQLHealthCheckResult.from(databaseHealthCheck))
            dependencyHealthChecks.add(GraphQLHealthCheckResult.from(schemaLoaderSystemHealthCheck))
        }
    }
}

/**
 * GraphQL query service for getting health status.
 */
class HealthQueryService : Query {
    fun getHealth() = HealthCheckService().getHealth()
}