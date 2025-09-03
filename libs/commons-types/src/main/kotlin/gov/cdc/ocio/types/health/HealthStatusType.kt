package gov.cdc.ocio.types.health

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


/**
 * Enumerates the possible health status values of a service.
 *
 * @property value [String] Health status type
 * @constructor
 */
@JsonSerialize(using = HealthStatusTypeSerializer::class)
@JsonDeserialize(using = HealthStatusTypeDeserializer::class)
@Serializable
enum class HealthStatusType(val value: String) {
    @SerialName("UP")
    STATUS_UP("UP"),
    @SerialName("DOWN")
    STATUS_DOWN("DOWN")
}
