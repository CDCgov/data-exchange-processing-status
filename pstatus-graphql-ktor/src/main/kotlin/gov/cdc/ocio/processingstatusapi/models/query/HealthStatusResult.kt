
package gov.cdc.ocio.processingstatusapi.models.query

import gov.cdc.ocio.types.health.HealthCheck
import kotlinx.serialization.Serializable

/**
 * The class which provides the health status result of each PS API service involved
 * @param status String
 * @param totalChecksDuration String
 * @param services List<HealthCheck>
 */
@Serializable
data class HealthStatusResult(
    val status: String,
    val totalChecksDuration: String,
    val services: List<HealthCheck>
)
