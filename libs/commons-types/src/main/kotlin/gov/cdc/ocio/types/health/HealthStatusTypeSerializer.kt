package gov.cdc.ocio.types.health

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
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

/**
 * Custom deserializer for the [HealthStatusType]
 */
class HealthStatusTypeDeserializer : JsonDeserializer<HealthStatusType>() {
    override fun deserialize(parser: com.fasterxml.jackson.core.JsonParser, ctxt: DeserializationContext): HealthStatusType {
        val value = parser.text
        return HealthStatusType.values().find { it.value == value }
            ?: throw IllegalArgumentException("Unknown health status: $value")
    }
}