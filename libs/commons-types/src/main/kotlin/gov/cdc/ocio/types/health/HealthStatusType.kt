package gov.cdc.ocio.types.health

import com.fasterxml.jackson.databind.annotation.JsonSerialize


/**
 * Enumerates the possible health status values of a service.
 *
 * @property value [String] Health status type
 * @constructor
 */
@JsonSerialize(using = HealthStatusTypeSerializer::class)
enum class HealthStatusType(val value: String) {
    STATUS_UP("UP"),
    STATUS_DOWN("DOWN"),
    STATUS_UNSUPPORTED("UNSUPPORTED")
}
