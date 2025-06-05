package gov.cdc.ocio.processingnotifications.query

import gov.cdc.ocio.database.models.StageAction
import gov.cdc.ocio.database.models.StageService
import gov.cdc.ocio.database.persistence.ProcessingStatusRepository
import gov.cdc.ocio.database.utils.SqlClauseBuilder
import gov.cdc.ocio.types.model.Status
import java.time.*
import java.time.format.DateTimeFormatter


class DeadlineCheckQuery private constructor(
    val repository: ProcessingStatusRepository,
    dataStreamId: String,
    dataStreamRoute: String,
    private val expectedJurisdictions: List<String>,
    private val deadlineTime: LocalTime,
): ReportQuery(repository, listOf(dataStreamId), listOf(dataStreamRoute), expectedJurisdictions) {

    private val formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")

    class Builder(private val repository: ProcessingStatusRepository): ReportQueryBuilder {
        private var dataStreamId: String = ""
        private var dataStreamRoute: String = ""
        private var deadlineTime: LocalTime = LocalTime.of(12, 0)
        private var expectedJurisdictions: List<String> = listOf()

        fun withDataStreamId(dataStreamId: String): Builder {
            this.dataStreamId = dataStreamId
            return this
        }

        fun withDataStreamRoute(dataStreamRoute: String): Builder {
            this.dataStreamRoute = dataStreamRoute
            return this
        }

        fun withExpectedJurisdictions(expectedJurisdictions: List<String>): Builder {
            this.expectedJurisdictions = expectedJurisdictions
            return this
        }

        fun withDeadlineTime(hourOfDayDeadline: LocalTime): Builder {
            this.deadlineTime = hourOfDayDeadline
            return this
        }

        override fun build() = DeadlineCheckQuery(
            repository,
            dataStreamId, dataStreamRoute,
            expectedJurisdictions,
            deadlineTime)
    }

    override fun buildSql(): String {
        val querySB = StringBuilder()

        // Get today's date in UTC
        val today = LocalDate.now(ZoneId.of("UTC"))

        val dateStart = today.atStartOfDay(ZoneOffset.UTC).format(formatter)
        val dateEnd = today.atTime(23, 59, 59).atZone(ZoneOffset.UTC).format(formatter)
        val entireDayTimeRangeWhereClause = SqlClauseBuilder
            .buildSqlClauseForDateRange(null, dateStart, dateEnd, cPrefix, timeFunc)

        querySB.append("""
            SELECT r.jurisdiction, MIN(r.dexIngestDateTime) AS earliestUpload
            FROM $collectionName $cVar 
            ${whereClause(prefix = "WHERE")} 
            AND ${cPrefix}stageInfo.service = '${StageService.UPLOAD_API}' 
            AND ${cPrefix}stageInfo.${cElFunc("action")} = '${StageAction.UPLOAD_COMPLETED}' 
            AND ${cPrefix}stageInfo.status = '${Status.SUCCESS}' 
            AND $entireDayTimeRangeWhereClause 
            GROUP BY r.jurisdiction;
            """.trimIndent()
        )

        return querySB.toString().trimIndent()
    }

    fun run(): DeadlineCompliance {
        val deadlineQueryResults = runQuery(DeadlineQueryResult::class.java)
        val foundJurisdictions = deadlineQueryResults.map { it.jurisdiction }
        val missingJurisdictions = expectedJurisdictions.filter { !foundJurisdictions.contains(it) }
        val deadline = LocalDate.now(ZoneId.of("UTC")).atTime(deadlineTime).toInstant(ZoneOffset.UTC)
        val lateJurisdictions = deadlineQueryResults.filter {
            it.earliestUpload.isAfter(deadline)
        }.map { it.jurisdiction }
        return DeadlineCompliance(
            missingJurisdictions,
            lateJurisdictions
        )
    }
}