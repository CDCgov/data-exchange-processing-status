package gov.cdc.ocio.processingnotifications.query

import gov.cdc.ocio.database.persistence.ProcessingStatusRepository
import gov.cdc.ocio.types.InstantRange
import java.time.LocalDate


/**
 * Abstract class that defines a special type of report query that includes a time filter based on the UTC based
 * local date provided.  This is in addition to the base report query, which optionally filters by data stream id,
 * data stream route and jurisdiction.
 *
 * @property utcDateToRun LocalDate
 * @constructor
 */
abstract class UtcTimeToRunReportQuery(
    repository: ProcessingStatusRepository,
    dataStreamIds: List<String>,
    dataStreamRoutes: List<String>,
    jurisdictions: List<String>,
    val utcDateToRun: LocalDate
): ReportQuery(repository, dataStreamIds, dataStreamRoutes, jurisdictions) {

    open class Builder<T : Builder<T>>(
        repository: ProcessingStatusRepository
    ) : ReportQuery.Builder<T>(repository) {

        var utcDateToRun: LocalDate = LocalDate.now()

        @Suppress("UNCHECKED_CAST")
        fun withUtcToRun(utcDateToRun: LocalDate): T {
            this.utcDateToRun = utcDateToRun
            return this as T
        }

        override fun build(): ReportQuery {
            throw NotImplementedError("This function must be implemented by derived classes")
        }
    }

    /**
     * Use this to append the where class that filters by the data stream ids, data stream routes, jurisdictions,
     * and date provided.
     *
     * @param utcDateToRun LocalDate
     * @return String
     */
    fun whereClause(
        utcDateToRun: LocalDate
    ): String {
        val instantRange = InstantRange.fromLocalDate(utcDateToRun)
        val startTime = timeFunc(instantRange.start.epochSecond)
        val endTime = timeFunc(instantRange.endInclusive.epochSecond)

        val querySB = StringBuilder()
        querySB.append(super.whereClause())

        querySB.append("""
                AND ${cPrefix}dexIngestDateTime >= $startTime
                AND ${cPrefix}dexIngestDateTime < $endTime
                """)

        return querySB.toString().trimIndent()
    }
}