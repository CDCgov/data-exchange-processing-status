package gov.cdc.ocio.types.adapters

import com.google.gson.*
import java.lang.reflect.Type
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter


class OffsetDateTimeTypeAdapter : JsonSerializer<OffsetDateTime>, JsonDeserializer<OffsetDateTime> {

    // Serialize OffsetDateTime to a string
    override fun serialize(src: OffsetDateTime?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return JsonPrimitive(src?.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
    }

    // Deserialize string to OffsetDateTime
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): OffsetDateTime? {
        return json?.asString?.let { OffsetDateTime.parse(it, DateTimeFormatter.ISO_OFFSET_DATE_TIME) }
    }
}
