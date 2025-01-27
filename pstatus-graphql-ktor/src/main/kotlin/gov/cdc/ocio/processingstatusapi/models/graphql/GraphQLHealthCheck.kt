package gov.cdc.ocio.processingstatusapi.models.graphql

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gov.cdc.ocio.types.health.HealthStatusType


/**
 * Run health checks for the service.
 *
 * @property status [HealthStatusType]?
 * @property totalChecksDuration [String]?
 * @property dependencyHealthChecks [MutableList]<[GraphQLHealthCheckResult]>
 */
@GraphQLDescription("Run health checks for the service")
class GraphQLHealthCheck {

    @GraphQLDescription("Overall status of the service")
    var status : HealthStatusType? = HealthStatusType.STATUS_DOWN

    @GraphQLDescription("Total time it took to evaluate the health of the service and its dependencies")
    var totalChecksDuration : String? = null

    @GraphQLDescription("Status of the service dependencies")
    var dependencyHealthChecks = mutableListOf<GraphQLHealthCheckResult>()
}