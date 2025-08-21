package gov.cdc.ocio.processingnotifications.query

/**
 * Defines the interface for building a SQL-like query.
 */
fun interface ReportQuerySqlBuilder {
    fun buildSql(): String
}