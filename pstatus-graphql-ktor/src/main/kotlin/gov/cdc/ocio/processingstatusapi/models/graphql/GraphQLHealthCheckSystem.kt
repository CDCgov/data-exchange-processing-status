package gov.cdc.ocio.processingstatusapi.models.graphql

import com.expediagroup.graphql.generator.annotations.GraphQLDescription

/**
 * HealthCheck object with  overall health of the graphql service and its dependencies
 */
@GraphQLDescription("HealthCheck object with the overall health of the graphql service and its dependencies")
class GraphQLHealthCheckSystem {

    @GraphQLDescription("Name of the service")
    var service: String? = null

    @GraphQLDescription("Status of the service")
    var status: String? = null

    @GraphQLDescription("Issue related to graphql service dependency")
    var healthIssues: String? = null
}