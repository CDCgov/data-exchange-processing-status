package gov.cdc.ocio.processingstatusapi.utils

import gov.cdc.ocio.processingstatusapi.exceptions.BadRequestException
import java.text.ParseException
import java.text.SimpleDateFormat

object DateUtils {

    private const val DATE_FORMAT = "yyyyMMdd'T'HHmmss'Z'"
    private val sdf = SimpleDateFormat(DATE_FORMAT)

    /**
     * Get the epoch time from a string provided.
     *
     * @param dateStr String
     * @param fieldName String
     * @return Long
     * @throws BadRequestException
     */
    @Throws(BadRequestException::class)
    fun getEpochFromDateString(dateStr: String, fieldName: String): Long {
        try {
            return sdf.parse(dateStr).time / 1000 // convert to secs from millisecs
        } catch (e: ParseException) {
            throw BadRequestException("Failed to parse $fieldName: $dateStr.  Format should be: ${DATE_FORMAT}.")
        }
    }
}