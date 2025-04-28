package gov.cdc.ocio.processingnotifications.query

import gov.cdc.ocio.database.models.StageAction
import gov.cdc.ocio.database.models.StageService
import gov.cdc.ocio.database.persistence.ProcessingStatusRepository
import gov.cdc.ocio.processingnotifications.utils.SqlClauseBuilder
import gov.cdc.ocio.types.model.Status
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter


class DeadlineCheckQuery private constructor(
    val repository: ProcessingStatusRepository,
    dataStreamIds: List<String>,
    dataStreamRoutes: List<String>,
    private val expectedJurisdictions: List<String>,
    private val deadlineTime: LocalTime,
): ReportQuery(repository, dataStreamIds, dataStreamRoutes, expectedJurisdictions) {

    private val formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")

    class Builder(repository: ProcessingStatusRepository): ReportQuery.Builder<Builder>(repository) {
        private var deadlineTime: LocalTime = LocalTime.of(12, 0)

        fun withDeadlineTime(hourOfDayDeadline: LocalTime): Builder {
            this.deadlineTime = hourOfDayDeadline
            return this
        }

        override fun build() = DeadlineCheckQuery(
            repository, dataStreamIds, dataStreamRoutes,
            listOf(),
            deadlineTime)
    }

    override fun buildSql(): String {
        val querySB = StringBuilder()

        // Get today's date in UTC
        val today = LocalDate.now(ZoneId.of("UTC"))

        val dateStart = today.atStartOfDay(ZoneOffset.UTC).format(formatter)
        val dateEnd = today.atTime(
            deadlineTime.hour,
            deadlineTime.minute,
            deadlineTime.second
        ).atZone(ZoneOffset.UTC).format(formatter)
        val timeRangeWhereClause = SqlClauseBuilder().buildSqlClauseForDateRange(null, dateStart, dateEnd, cPrefix)

        querySB.append("""
            SELECT ${cPrefix}jurisdiction
            FROM $collectionName $cVar
            """.trimIndent()
        )
        querySB.append(whereClause())
//            AND ${cPrefix}jurisdiction NOT IN (${expectedJurisdictions.joinToString { "'$it'" }})        
        querySB.append("""
            AND ${cPrefix}stageInfo.service = '${StageService.UPLOAD_API}'
            AND ${cPrefix}stageInfo.${cElFunc("action")} = '${StageAction.UPLOAD_COMPLETED}'
            AND ${cPrefix}stageInfo.status = '${Status.SUCCESS}'
            """.trimIndent()
        )

        querySB.append(whereClause())
        querySB.append("AND $timeRangeWhereClause")

        return querySB.toString().trimIndent()
    }

    fun run(): List<String> {
        val foundJurisdictions = runQuery(String::class.java)
        val missingJurisdictions = expectedJurisdictions.filter { !foundJurisdictions.contains(it) }
        return missingJurisdictions.toList()
    }
}