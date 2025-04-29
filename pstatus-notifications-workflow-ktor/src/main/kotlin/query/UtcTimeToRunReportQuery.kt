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
     * Constructs a SQL WHERE clause for filtering data based on a specified UTC date range.
     *
     * Generates a SQL fragment using the provided date as a range filter for the `dexIngestDateTime` field.
     * The clause is prefixed with "WHERE" or "AND" based on whether it is the first clause of the query.
     *
     * @param utcDateToRun The `LocalDate` representing the UTC date that defines the range to filter data.
     *                     The filtering range will be set from the start to the end of this date in UTC timezone.
     * @param isFirstClause A Boolean indicating whether this is the first clause in the query. If true,
     *                      the clause is prefixed with "WHERE"; if false, it is prefixed with "AND".
     * @return A `String` representing the constructed SQL WHERE clause with the specified date range conditions.
     */
    fun whereClause(
        utcDateToRun: LocalDate,
        isFirstClause: Boolean = false,
    ): String {
        val instantRange = InstantRange.fromLocalDate(utcDateToRun)
        val startTime = timeFunc(instantRange.start.epochSecond)
        val endTime = timeFunc(instantRange.endInclusive.epochSecond)

        val querySB = StringBuilder()
        querySB.append(super.whereClause(isFirstClause))

        querySB.append("""
                AND ${cPrefix}dexIngestDateTime >= $startTime
                AND ${cPrefix}dexIngestDateTime < $endTime
                """)

        return querySB.toString().trimIndent()
    }
}