package gov.cdc.ocio.processingnotifications.query

import gov.cdc.ocio.database.persistence.ProcessingStatusRepository
import gov.cdc.ocio.processingnotifications.utils.SqlClauseBuilder
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter


class DeadlineCheckQuery private constructor(
    val repository: ProcessingStatusRepository,
    dataStreamIds: List<String>,
    dataStreamRoutes: List<String>,
    private val expectedJurisdictions: List<String>,
    private val utcDeadline: LocalDate,
): ReportQuery(repository, dataStreamIds, dataStreamRoutes, listOf()) {

    class Builder(repository: ProcessingStatusRepository): ReportQuery.Builder<Builder>(repository) {
        private var utcDeadline: LocalDate = LocalDate.now()

        fun withUtcDeadline(utcDeadline: LocalDate): Builder {
            this.utcDeadline = utcDeadline
            return this
        }

        override fun build() = DeadlineCheckQuery(
            repository, dataStreamIds, dataStreamRoutes,
            listOf(),
            utcDeadline)
    }

    override fun buildSql(): String {
        val querySB = StringBuilder()

        /** Get today's date in UTC **/
        val today = LocalDate.now(ZoneId.of("UTC"))
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")

        val dateStart = today.atStartOfDay(ZoneOffset.UTC).format(formatter)
        val dateEnd = today.atTime(12, 0, 0).atZone(ZoneOffset.UTC).format(formatter)
        val timeRangeWhereClause = SqlClauseBuilder().buildSqlClauseForDateRange(null, dateStart, dateEnd, cPrefix)

        val notificationQuery = """
           SELECT ${cPrefix}uploadId
           FROM $collectionName $cVar
           """.trimIndent()

        querySB.append(notificationQuery)
        querySB.append(whereClause())
        querySB.append("AND $timeRangeWhereClause")

        querySB.append("""
                GROUP BY ${cPrefix}uploadId
            ) AS upload_metrics;
        """)

        return querySB.toString().trimIndent()
    }

    fun run() = runQuery(String::class.java)
}