package gov.cdc.ocio.processingstatusapi.utils

import gov.cdc.ocio.processingstatusapi.exceptions.BadRequestException
import org.apache.commons.lang3.time.FastDateFormat
import org.joda.time.DateTimeZone
import java.text.ParseException
import java.time.Instant
import java.time.OffsetDateTime
import java.util.*

object DateUtils {

    private const val DATE_FORMAT = "yyyyMMdd'T'HHmmss'Z'"

    private val dateFormat = FastDateFormat.getInstance(DATE_FORMAT)

    /**
     * Get the epoch time from a string provided.
     *
     * @param dateStr String
     * @param fieldName String
     * @return Long - Epoch time in milliseconds
     * @throws BadRequestException
     */
    @Throws(BadRequestException::class)
    fun getEpochFromDateString(dateStr: String, fieldName: String): Long {
        try {
            // Parse the date string to a Date object
            val date = dateFormat.parse(dateStr)
            return date.time
        } catch (e: ParseException) {
            throw BadRequestException("Failed to parse $fieldName: $dateStr.  Format should be: ${DATE_FORMAT}.")
        }
    }

    /**
     * Get OffsetDateTime from Instant
     * @param dateInstant Instant
     * @return OffSetDateTime
     *
     */

    @Throws(BadRequestException::class)
    fun getOffsetDateTimeFromInstant(dateInstant: Instant?, fieldName: String): OffsetDateTime {
        try {
            if(dateInstant == null)
                throw NullPointerException("The $fieldName value is null")

           val offsetDateTime =  OffsetDateTime.parse(org.joda.time.Instant.ofEpochMilli(dateInstant.epochSecond)
               .toDateTime(DateTimeZone.UTC).toString())
            return offsetDateTime
        } catch (e: ParseException) {
            throw BadRequestException("Failed to parse $dateInstant.  Format should be of type Instant.")
        }
    }
}