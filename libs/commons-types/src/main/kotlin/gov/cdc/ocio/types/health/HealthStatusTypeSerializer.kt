package gov.cdc.ocio.types.health

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider


/**
 * Custom serializer for the [HealthStatusType]
 */
class HealthStatusTypeSerializer : JsonSerializer<HealthStatusType>() {

    /**
     * Jackson serializer for the [HealthStatusType] type.
     *
     * @param type HealthStatusType
     * @param gen JsonGenerator
     * @param serializers SerializerProvider
     */
    override fun serialize(type: HealthStatusType, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeString(type.value)
    }
}