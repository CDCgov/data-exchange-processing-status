package gov.cdc.ocio.processingnotifications.query

import gov.cdc.ocio.database.persistence.ProcessingStatusRepository
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
    name: String,
    repository: ProcessingStatusRepository,
    dataStreamIds: List<String>,
    dataStreamRoutes: List<String>,
    jurisdictions: List<String>,
    val utcDateToRun: LocalDate
): ReportQuery(name, repository, dataStreamIds, dataStreamRoutes, jurisdictions) {

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
}