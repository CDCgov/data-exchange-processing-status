package gov.cdc.ocio.processingnotifications.query

import java.time.Instant


/**
 * Represents the result of a query determining the earliest data upload time
 * for a specific jurisdiction.
 *
 * @property jurisdiction The jurisdiction identifier associated with the query result.
 * @property earliestUpload The earliest upload datetime for the specified jurisdiction.
 */
data class DeadlineQueryResult(
    val jurisdiction: String,
    val earliestUpload: Instant
)