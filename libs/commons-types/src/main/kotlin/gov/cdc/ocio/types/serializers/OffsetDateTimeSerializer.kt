package gov.cdc.ocio.types.serializers

import java.time.OffsetDateTime
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import java.time.format.DateTimeFormatter


/**
 * Kotlinx serializer for the OffsetDateTime type.
 */
object OffsetDateTimeSerializer : KSerializer<OffsetDateTime> {
    override val descriptor =
        PrimitiveSerialDescriptor("OffsetDateTime", PrimitiveKind.STRING)

    /**
     * Serialize the provided [OffsetDateTime].
     *
     * @param encoder Encoder
     * @param value OffsetDateTime
     */
    override fun serialize(encoder: Encoder, value: OffsetDateTime) {
        encoder.encodeString(value.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
    }

    /**
     * Deserialize to an [OffsetDateTime].
     *
     * @param decoder Decoder
     * @return OffsetDateTime
     */
    override fun deserialize(decoder: Decoder): OffsetDateTime {
        return OffsetDateTime.parse(decoder.decodeString())
    }
}
