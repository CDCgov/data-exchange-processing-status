package gov.cdc.ocio.types.health

/**
 * Health check result.
 *
 * @property system String
 * @property service String
 * @property status HealthStatusType
 * @property healthIssues String?
 * @constructor
 */
import kotlinx.serialization.Serializable

@Serializable
data class HealthCheckResult(
    val system: String,
    val service: String?,
    val status: HealthStatusType,
    val healthIssues: String? = null
)