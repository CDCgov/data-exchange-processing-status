package gov.cdc.ocio.types.health


/**
 * Run health checks for the service.
 *
 *  @param name String
 *  @param status HealthStatusType
 * @param totalChecksDuration String?
 * @param dependencyHealthChecks MutableList<HealthCheckSystem>
 */
import kotlinx.serialization.Serializable

@Serializable
data class HealthCheck(
     var name:String?= null,
     var status:HealthStatusType = HealthStatusType.STATUS_DOWN,
     var totalChecksDuration: String? = null,
     var dependencyHealthChecks:MutableList<HealthCheckResult> = mutableListOf())



