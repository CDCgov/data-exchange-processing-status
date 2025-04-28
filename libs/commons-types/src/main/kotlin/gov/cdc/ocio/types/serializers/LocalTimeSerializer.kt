package gov.cdc.ocio.types.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalTime


/**
 * Kotlinx serializer for the `LocalTime` type.
 *
 * This object facilitates serialization and deserialization of `LocalTime` instances
 * to and from their ISO-8601 string representation. During serialization, the `LocalTime`
 * value is converted to a string using its `toString()` method. Conversely, during
 * deserialization, the string is parsed back into a `LocalTime` object.
 */
object LocalTimeSerializer : KSerializer<LocalTime> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("LocalTime", PrimitiveKind.STRING)

    /**
     * Serializes a [LocalTime] instance into its string representation.
     *
     * @param encoder The encoder used to encode the value.
     * @param value The [LocalTime] value to be serialized.
     */
    override fun serialize(encoder: Encoder, value: LocalTime) {
        encoder.encodeString(value.toString()) // e.g., "12:30:00"
    }

    /**
     * Deserializes a string representation of a `LocalTime` into a [LocalTime] object.
     *
     * @param decoder The decoder used to decode the string value.
     * @return The deserialized [LocalTime] object.
     */
    override fun deserialize(decoder: Decoder): LocalTime {
        return LocalTime.parse(decoder.decodeString())
    }
}