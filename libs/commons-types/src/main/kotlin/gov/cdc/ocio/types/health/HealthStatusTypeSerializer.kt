package gov.cdc.ocio.types.health

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer


/**
 * Custom serializer for the [HealthStatusType]
 */
class HealthStatusTypeSerializer : StdSerializer<HealthStatusType>(HealthStatusType::class.java) {

    /**
     * Implementation of the serializer for [HealthStatusType].
     *
     * @param type HealthStatusType
     * @param gen JsonGenerator
     * @param provider SerializerProvider
     */
    override fun serialize(type: HealthStatusType, gen: JsonGenerator, provider: SerializerProvider) {
        gen.writeString(type.value)
    }
}