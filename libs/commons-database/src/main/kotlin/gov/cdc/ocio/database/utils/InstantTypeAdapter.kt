package gov.cdc.ocio.database.utils

import com.google.gson.*
import java.lang.reflect.Type
import java.time.Instant


/**
 * Instant type adaptor used by gson
 */
class InstantTypeAdapter :
    JsonSerializer<Instant?>,
    JsonDeserializer<Instant?>
{
    /**
     * Serialize implementation for the Instant type.
     *
     * @param date Instant?
     * @param typeOfSrc Type
     * @param context JsonSerializationContext
     * @return JsonElement
     */
    override fun serialize(date: Instant?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return JsonPrimitive(date?.toEpochMilli())
    }

    /**
     * Deserialize implementation for the Instant type.
     *
     * @param json JsonElement
     * @param typeOfT Type
     * @param context JsonDeserializationContext
     * @return Instant?
     */
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Instant? {
        return json?.asLong?.let { Instant.ofEpochMilli(it) }
    }

}