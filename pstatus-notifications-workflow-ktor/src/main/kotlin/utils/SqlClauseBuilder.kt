package gov.cdc.ocio.processingnotifications.utils

import com.azure.cosmos.implementation.BadRequestException
import org.joda.time.DateTime
import org.joda.time.DateTimeZone

/**
 *Builds a SQL clause for filtering timestamps based on a specified date range.
 */
class SqlClauseBuilder {
    /**
     * @param daysInterval An optional number of days to subtract from today's date.
     * @param dateStart An optional start date in string format.
     * @param dateEnd An optional end date in string format.
     * @return A SQL clause as a String.
     */
    @Throws(NumberFormatException::class, BadRequestException::class)
    fun buildSqlClauseForDateRange(daysInterval: Int?,
                                   dateStart: String?,
                                   dateEnd: String?,  cPrefix: String): String {

        val timeRangeSqlPortion = StringBuilder()
        if (daysInterval != null) {
            val dateStartEpochSecs = DateTime
                .now(DateTimeZone.UTC)
                .minusDays(daysInterval)
                .withTimeAtStartOfDay()
                .toDate()
                .time / 1000
            timeRangeSqlPortion.append("r.timestamp >= $dateStartEpochSecs")
        } else {
            dateStart?.run {
                val dateStartEpochSecs = DateUtils.getEpochFromDateString(dateStart, "date_start")
                timeRangeSqlPortion.append("${cPrefix}dexIngestDateTime >= $dateStartEpochSecs")
            }
            dateEnd?.run {
                val dateEndEpochSecs = DateUtils.getEpochFromDateString(dateEnd, "date_end")
                timeRangeSqlPortion.append(" and ${cPrefix}dexIngestDateTime < $dateEndEpochSecs")
            }
        }
        return timeRangeSqlPortion.toString()
    }
}