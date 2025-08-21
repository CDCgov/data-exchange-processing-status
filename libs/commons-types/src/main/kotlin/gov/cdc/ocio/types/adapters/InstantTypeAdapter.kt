package gov.cdc.ocio.types.adapters

import com.google.gson.*
import java.lang.reflect.Type
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter


/**
 * Instant type adaptor used by gson
 */
class InstantTypeAdapter(private val asEpoch: Boolean = true) :
    JsonSerializer<Instant?>,
    JsonDeserializer<Instant?>
{
    private val formatter = DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC)

    /**
     * Serialize implementation for the Instant type.
     *
     * @param date Instant?
     * @param typeOfSrc Type
     * @param context JsonSerializationContext
     * @return JsonElement
     */
    override fun serialize(date: Instant?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return when (asEpoch) {
            true -> JsonPrimitive(date?.toEpochMilli())
            false -> JsonPrimitive(date?.let { formatter.format(it) })
        }
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