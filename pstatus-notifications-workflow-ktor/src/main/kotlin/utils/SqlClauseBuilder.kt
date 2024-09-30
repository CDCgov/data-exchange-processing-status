package gov.cdc.ocio.processingnotifications.utils

import com.azure.cosmos.implementation.BadRequestException
import org.joda.time.DateTime
import org.joda.time.DateTimeZone

class SqlClauseBuilder {

    @Throws(NumberFormatException::class, BadRequestException::class)
    fun buildSqlClauseForDateRange(daysInterval: Int?,
                                   dateStart: String?,
                                   dateEnd: String?): String {

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
                timeRangeSqlPortion.append("r.timestamp >= $dateStartEpochSecs")
            }
            dateEnd?.run {
                val dateEndEpochSecs = DateUtils.getEpochFromDateString(dateEnd, "date_end")
                timeRangeSqlPortion.append(" and r.timestamp < $dateEndEpochSecs")
            }
        }
        return timeRangeSqlPortion.toString()
    }
}