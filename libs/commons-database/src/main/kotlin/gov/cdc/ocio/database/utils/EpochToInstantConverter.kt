package gov.cdc.ocio.database.utils

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import java.time.Instant
import java.time.format.DateTimeParseException

/**
 * The custom converter from epoch to Instant. This is mainly needed for cosmos
 */
class EpochToInstantConverter : JsonDeserializer<Instant>() {
    /**
     * The function for converting epoch to Instant
     * : For epoch timestamps, it checks if the value is in seconds or milliseconds.
     * For ISO-8601 formatted strings, Instant.parse() is used directly.
     */
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Instant {
        return try {
            when {
                // Handle numeric epoch timestamp
                p.currentToken.isNumeric -> {
                    val epoch = p.longValue
                    // Convert to milliseconds if the epoch is likely in seconds
                    if (epoch < 1_000_000_000_000L) {
                        Instant.ofEpochSecond(epoch)
                    } else {
                        Instant.ofEpochMilli(epoch)
                    }
                }
                // Handle ISO-8601 formatted string
                else -> Instant.parse(p.text)
            }
        } catch (e: DateTimeParseException) {
            throw IllegalArgumentException("Failed to parse '${p.text}' or '${p.longValue}'. Expected format: ISO-8601 or epoch milliseconds/seconds.")
        }
    }
}




