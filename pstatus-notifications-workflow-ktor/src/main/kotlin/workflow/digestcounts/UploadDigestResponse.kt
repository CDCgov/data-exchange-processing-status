package gov.cdc.ocio.processingnotifications.workflow.digestcounts

import java.time.Instant


/**
 * Represents the response data for an upload digest, containing metadata and status counts.
 *
 * @property dataStreamId Identifier for the data stream.
 * @property dataStreamRoute Route associated with the data stream.
 * @property jurisdiction Jurisdiction associated with the data stream.
 * @property started Count of uploads that have started.
 * @property completed Count of completed uploads.
 * @property failedDelivery Count of upload deliveries that failed.
 * @property delivered Count of successfully delivered uploads.
 * @property lastUploadCompletedTime The timestamp of the most recent completed upload, if available.
 */
data class UploadDigestResponse(
    var dataStreamId: String = "",
    var dataStreamRoute: String = "",
    var jurisdiction: String = "",
    var started: Int = 0,
    var completed: Int = 0,
    var failedDelivery: Int = 0,
    var delivered: Int = 0,
    var lastUploadCompletedTime: Instant? = null
)