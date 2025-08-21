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
     * Constructs a SQL where clause for filtering data based on a specific UTC date.
     * The generated clause includes conditions for a datetime range, calculated from the provided date,
     * and appends it to the parent `whereClause` method result.
     *
     * @param utcDateToRun The date for which the SQL query will filter records. Assumes UTC timezone.
     * @param prefix The prefix for the where clause. Defaults to "WHERE".
     * @return A SQL where clause as a string containing conditions for the specified UTC date range.
     */
    fun whereClause(
        utcDateToRun: LocalDate,
        prefix: String = "WHERE",
    ): String {
        val instantRange = InstantRange.fromLocalDate(utcDateToRun)
        val startTime = timeFunc(instantRange.start.epochSecond)
        val endTime = timeFunc(instantRange.endInclusive.epochSecond)

        val querySB = StringBuilder()
        querySB.append(super.whereClause(prefix))

        querySB.append("""
                AND ${cPrefix}dexIngestDateTime >= $startTime
                AND ${cPrefix}dexIngestDateTime < $endTime
                """)

        return querySB.toString().trimIndent()
    }
}