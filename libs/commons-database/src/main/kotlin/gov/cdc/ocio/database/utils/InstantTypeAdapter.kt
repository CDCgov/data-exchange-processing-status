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
        // We can't determine the type of the source through inspection of the JsonElement other than to try to get it
        // as different types.  Keep trying until one succeeds, otherwise throw an exception.
        runCatching { json?.asString?.let { return Instant.parse(it) } }
        runCatching { json?.asLong?.let { return Instant.ofEpochMilli(it) } }
        throw JsonParseException("Failed to parse JsonElement while deserializing ${typeOfT?.typeName}; json = ${json.toString()}")
    }
}