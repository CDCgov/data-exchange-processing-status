package gov.cdc.ocio.processingstatusapi.models.graphql

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gov.cdc.ocio.types.health.HealthCheckResult


/**
 * HealthCheck object with  overall health of the graphql service and its dependencies
 */
@GraphQLDescription("HealthCheck object with the overall health of the graphql service and its dependencies")
class GraphQLHealthCheckResult {

    @GraphQLDescription("Name of the system")
    var system: String? = null

    @GraphQLDescription("Name of the service implementing the system")
    var service: String? = null

    @GraphQLDescription("Status of the service")
    var status: String? = null

    @GraphQLDescription("Issue related to graphql service dependency")
    var healthIssues: String? = null

    companion object {
        /**
         * Convenience function to convert a [HealthCheckResult] object to a [GraphQLHealthCheckResult] object.
         *
         * @param healthCheckResult HealthCheckResult
         * @return GraphQLHealthCheckResult
         */
        fun from(healthCheckResult: HealthCheckResult) = GraphQLHealthCheckResult().apply {
            this.system = healthCheckResult.system
            this.service = healthCheckResult.service
            this.status = healthCheckResult.status.value
            this.healthIssues = healthCheckResult.healthIssues
        }
    }
}