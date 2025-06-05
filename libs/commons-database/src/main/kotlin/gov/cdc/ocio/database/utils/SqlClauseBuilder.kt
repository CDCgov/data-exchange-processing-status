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
            timeRangeSqlPortion.append("${cPrefix}dexIngestDateTime >= ${timeFunc(dateStartEpochSecs)}")
        } else {
            dateStart?.let {
                val dateStartEpochSecs = DateUtils.getEpochFromDateString(it)
                timeRangeSqlPortion.append("${cPrefix}dexIngestDateTime >= ${timeFunc(dateStartEpochSecs)}")
            }
            dateEnd?.let {
                val dateEndEpochSecs = DateUtils.getEpochFromDateString(it)
                timeRangeSqlPortion.append(" AND ${cPrefix}dexIngestDateTime < ${timeFunc(dateEndEpochSecs)}")
            }
        }
        return timeRangeSqlPortion.toString()
    }

    /**
     * Builds an SQL clause for filtering results based on a relative date range defined by a days interval.
     * Utilizes a custom time conversion function and column prefix for constructing the SQL clause.
     *
     * @param daysInterval The number of days for the relative date range.
     *                     If not null, this is used to calculate the start date relative to the current time.
     *                     If null, no SQL clause for a date range is constructed.
     * @param cPrefix A prefix to be concatenated to the SQL column name for date filtering.
     * @param timeFunc A function that converts epoch seconds to the desired SQL representation of the timestamp,
     *                 used in constructing the SQL clause.
     * @throws NumberFormatException If the provided days interval causes an invalid number conversion.
     * @throws BadRequestException If there is an issue with constructing the SQL clause.
     */
    @Throws(NumberFormatException::class, BadRequestException::class)
    fun buildSqlClauseForDaysInterval(
        daysInterval: Int?,
        cPrefix: String,
        timeFunc: (Long) -> String
    ) = buildSqlClauseForDateRange(daysInterval, null, null, cPrefix, timeFunc)
}