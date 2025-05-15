package gov.cdc.ocio.processingnotifications.workflow.deadlinecheck

import java.time.Instant


/**
 * Represents summary information for a jurisdiction's uploads.
 *
 * This data class contains metadata related to the upload activity of a specific jurisdiction,
 * including the total count of uploads and the timestamp of the last upload.
 *
 * @property count The total number of uploads performed by the jurisdiction.
 * @property lastUpload The timestamp of the most recent upload by the jurisdiction, or null if no upload has been recorded.
 */
data class JurisdictionFacts(val count: Int, val lastUpload: Instant?)