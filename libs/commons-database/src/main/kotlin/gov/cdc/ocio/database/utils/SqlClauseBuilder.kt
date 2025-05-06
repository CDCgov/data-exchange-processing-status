package gov.cdc.ocio.database.utils

import gov.cdc.ocio.types.utils.DateUtils
import io.ktor.server.plugins.*
import org.joda.time.DateTime
import org.joda.time.DateTimeZone


/**
 * Builds a SQL clause for filtering timestamps based on a specified date range.
 */
object SqlClauseBuilder {

    /**
     * Builds an SQL clause for filtering results based on a date range or interval.
     * Constructs the SQL condition for filtering based on the provided parameters:
     * either a `daysInterval` for a relative date range or `dateStart` and `dateEnd` for a specific range.
     *
     * @param daysInterval The number of days for the relative date range. If provided, it overrides `dateStart` and `dateEnd`.
     * @param dateStart A string representation of the start date for the range filter. Ignored if `daysInterval` is specified.
     * @param dateEnd A string representation of the end date for the range filter. Ignored if `daysInterval` is specified.
     * @param cPrefix A prefix to be added to the SQL column name for the date filter.
     * @param timeFunc A function that converts epoch seconds to the desired SQL representation of the timestamp.
     * @return A string representing the SQL clause for the date range or interval filter.
     * @throws NumberFormatException If invalid parameters cause number formatting issues.
     * @throws BadRequestException If the date parsing fails or invalid input data.
     */
    @Throws(NumberFormatException::class, BadRequestException::class)
    fun buildSqlClauseForDateRange(
        daysInterval: Int?,
        dateStart: String?,
        dateEnd: String?,
        cPrefix: String,
        timeFunc: (Long) -> String
    ): String {
        val timeRangeSqlPortion = StringBuilder()
        if (daysInterval != null) {
            val dateStartEpochSecs = DateTime
                .now(DateTimeZone.UTC)
                .minusDays(daysInterval)
                .withTimeAtStartOfDay()
                .toDate()
                .time / 1000
            timeRangeSqlPortion.append("r.dexIngestDateTime >= ${timeFunc(dateStartEpochSecs)}")
        } else {
            dateStart?.run {
                val dateStartEpochSecs = DateUtils.getEpochFromDateString(dateStart)
                timeRangeSqlPortion.append("${cPrefix}dexIngestDateTime >= ${timeFunc(dateStartEpochSecs)}")
            }
            dateEnd?.run {
                val dateEndEpochSecs = DateUtils.getEpochFromDateString(dateEnd)
                timeRangeSqlPortion.append(" and ${cPrefix}dexIngestDateTime < ${timeFunc(dateEndEpochSecs)}")
            }
        }
        return timeRangeSqlPortion.toString()
    }
}