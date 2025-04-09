package gov.cdc.ocio.processingnotifications.workflow.digestcounts


// Map jurisdiction to counts
typealias CountsByJurisdiction = Map<String, Counts>

// Map data stream route to jurisdiction
typealias CountsByDataStreamRoute = Map<String, CountsByJurisdiction>

// Map data stream id to data stream route
typealias CountsByDataStreamId = Map<String, CountsByDataStreamRoute>

/**
 * Digest of counts by data stream, grouped by data stream id, route and jurisdiction.
 *
 * @property digest Map<String, Map<String, Map<String, Counts>>>
 * @constructor
 */
data class UploadDigestCounts(
    val digest: CountsByDataStreamId
)
