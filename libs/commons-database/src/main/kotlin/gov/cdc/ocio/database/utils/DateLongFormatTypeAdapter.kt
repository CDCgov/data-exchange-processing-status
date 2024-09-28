package gov.cdc.ocio.database.utils

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.io.IOException
import java.util.*

class DateLongFormatTypeAdapter : TypeAdapter<Date?>() {
    @Throws(IOException::class)
    override fun write(out: JsonWriter, value: Date?) {
        if (value != null) out.value(value.time)
        else out.nullValue()
    }

    @Throws(IOException::class)
    override fun read(`in`: JsonReader): Date {
        return Date(`in`.nextLong())
    }
}