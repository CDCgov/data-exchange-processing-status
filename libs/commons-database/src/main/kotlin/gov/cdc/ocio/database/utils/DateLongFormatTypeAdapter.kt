package gov.cdc.ocio.database.utils

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.io.IOException
import java.util.*

/**
 * Format type adapter for the date long format.
 */
class DateLongFormatTypeAdapter : TypeAdapter<Date?>() {

    /**
     * Override the write implementation for a Date, translating a Date into a long (epoch in milliseconds).
     *
     * @param out JsonWriter
     * @param value Date?
     * @throws IOException
     */
    @Throws(IOException::class)
    override fun write(out: JsonWriter, value: Date?) {
        if (value != null) out.value(value.time)
        else out.nullValue()
    }

    /**
     * Override the read implementation for translation a long into a date.
     * @param `in` JsonReader
     * @return Date
     * @throws IOException
     */
    @Throws(IOException::class)
    override fun read(`in`: JsonReader): Date {
        return Date(`in`.nextLong())
    }
}