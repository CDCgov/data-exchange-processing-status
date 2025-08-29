package gov.cdc.ocio.types.utils

import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * A utility class for handling date operations.
 */
object DateUtils {

    private const val DATE_FORMAT = "yyyyMMdd'T'HHmmss'Z'"

    private val dateFormatter = DateTimeFormatter.ofPattern(DATE_FORMAT)

    /**
     * Get the epoch time from a string provided.
     *
     * @param dateStr String
     * @return Long - Epoch time in milliseconds
     * @throws IllegalArgumentException
     */
    @Throws(IllegalArgumentException::class)
    fun getEpochFromDateString(dateStr: String): Long {
        return try {
            val localDateTime = LocalDateTime.parse(dateStr, dateFormatter)
            return localDateTime.toEpochSecond(ZoneOffset.UTC) * 1000
        } catch (e: DateTimeParseException) {
            throw IllegalArgumentException("Failed to parse $dateStr as a date. Format should be: ${DATE_FORMAT}.")
        }
    }

}