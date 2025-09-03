package gov.cdc.ocio.processingstatusapi.models.query

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


class GraphQLHealth : KoinComponent
 {
    private val logger = KotlinLogging.logger {}

    private val repository by inject<ProcessingStatusRepository>()

    private val schemaLoader by inject<SchemaLoader>()

    fun getHealth(): GraphQLHealthCheck {
        val databaseHealthCheck: HealthCheckResult
        val schemaLoaderSystemHealthCheck: HealthCheckResult

        val time = measureTimeMillis {
            databaseHealthCheck = repository.healthCheckSystem.doHealthCheck()
            schemaLoaderSystemHealthCheck = schemaLoader.healthCheckSystem.doHealthCheck()
        }

        return GraphQLHealthCheck().apply {
            status = if (databaseHealthCheck.status == HealthStatusType.STATUS_UP &&
                schemaLoaderSystemHealthCheck.status == HealthStatusType.STATUS_UP
            ) {
                HealthStatusType.STATUS_UP
            } else {
                HealthStatusType.STATUS_DOWN
            }
            totalChecksDuration = TimeUtils.formatMillisToHMS(time)
            dependencyHealthChecks.add(GraphQLHealthCheckResult.from(databaseHealthCheck))
            dependencyHealthChecks.add(GraphQLHealthCheckResult.from(schemaLoaderSystemHealthCheck))
        }
    }
}
