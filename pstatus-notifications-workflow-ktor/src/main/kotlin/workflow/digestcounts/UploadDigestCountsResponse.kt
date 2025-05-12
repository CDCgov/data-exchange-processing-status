package gov.cdc.ocio.processingnotifications.workflow.digestcounts

import gov.cdc.ocio.processingnotifications.activity.DataResponse


/**
 * Represents the response containing aggregated upload digest counts, metrics, and timing details.
 *
 * This data class provides a comprehensive view of the upload processing response, including:
 * - Aggregated counts grouped by data stream, route, and jurisdiction.
 * - Detailed upload metrics such as timing and file size statistics.
 * - A list capturing durations of individual upload processes.
 *
 * @property aggregatedCounts Aggregated counts of upload and delivery statuses, grouped by
 *                             data stream id, route, and jurisdiction.
 * @property uploadMetrics Metrics related to upload and delivery timings and file properties.
 * @property uploadDurations List of durations for individual uploads, measured in milliseconds.
 */
data class UploadDigestCountsResponse(
    val aggregatedCounts: UploadDigestCounts,
    val uploadMetrics: UploadMetrics,
    val uploadDurations: List<Long>
) : DataResponse