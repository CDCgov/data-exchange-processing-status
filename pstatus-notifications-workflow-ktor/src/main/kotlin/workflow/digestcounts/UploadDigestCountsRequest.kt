package gov.cdc.ocio.processingnotifications.workflow.digestcounts

import gov.cdc.ocio.processingnotifications.activity.DataRequest
import java.time.LocalDate


/**
 * Represents a request to upload digest counts for data streams.
 *
 * This request contains information about the data streams, routes, jurisdictions,
 * a specific time frame, and the targeted date for processing.
 *
 * @property dataStreamIds List of identifiers for the data streams to be included in this request.
 * @property dataStreamRoutes List of routes associated with the specified data streams.
 * @property jurisdictions List of jurisdictions applicable to the data streams.
 * @property sinceDays Specifies the number of days in the past to consider for the digest.
 * @property utcDateToRun The specific UTC date on which the processing should be executed.
 */
data class UploadDigestCountsRequest(
    val dataStreamIds: List<String>,
    val dataStreamRoutes: List<String>,
    val jurisdictions: List<String>,
    val sinceDays: Int,
    val utcDateToRun: LocalDate
) : DataRequest