package gov.cdc.ocio.processingnotifications.utils

import com.azure.cosmos.implementation.BadRequestException
import org.apache.commons.lang3.time.FastDateFormat
import java.text.ParseException
import java.util.*

/**
 * A utility class for handling date operations.
 */
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

}