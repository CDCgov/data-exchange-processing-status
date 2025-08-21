package gov.cdc.ocio.processingnotifications.query

/**
 * Defines the interface for building a report query.  The output of the build function should be specialized
 * report query.
 */
fun interface ReportQueryBuilder {
    fun build(): ReportQuery
}