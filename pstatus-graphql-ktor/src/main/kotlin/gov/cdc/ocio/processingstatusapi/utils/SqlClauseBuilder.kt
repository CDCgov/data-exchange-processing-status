package gov.cdc.ocio.processingstatusapi.utils

import gov.cdc.ocio.processingstatusapi.exceptions.BadRequestException
import org.joda.time.DateTime
import org.joda.time.DateTimeZone


class SqlClauseBuilder {

    @Throws(NumberFormatException::class, BadRequestException::class)
    fun buildSqlClauseForDateRange(
        daysInterval: Int?,
        dateStart: String?,
        dateEnd: String?,
        cPrefix: String
    ): String {
        val timeRangeSqlPortion = StringBuilder()
        if (daysInterval != null) {
            val dateStartEpochSecs = DateTime
                .now(DateTimeZone.UTC)
                .minusDays(daysInterval)
                .withTimeAtStartOfDay()
                .toDate()
                .time / 1000
            timeRangeSqlPortion.append("${cPrefix}dexIngestDateTime >= $dateStartEpochSecs")
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