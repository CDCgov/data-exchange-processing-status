package gov.cdc.ocio.processingstatusapi.models.query

import gov.cdc.ocio.types.health.HealthStatusType

data class DependencyHealthCheck(
    val system: String,
    val service: String,
    val status: String?,
    val healthIssues: String?
)

data class HealthCheck(
    val name: String,
    val status: HealthStatusType,
    val totalChecksDuration: String,
    val dependencyHealthChecks: List<DependencyHealthCheck>
)

data class HealthResponse(
    val status: String,
    val totalChecksDuration: String,
    val services: List<HealthCheck>
)
