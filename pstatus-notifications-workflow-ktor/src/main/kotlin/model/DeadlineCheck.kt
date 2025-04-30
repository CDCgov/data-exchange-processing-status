package gov.cdc.ocio.processingnotifications.model

import gov.cdc.ocio.processingnotifications.workflow.deadlinecheck.JurisdictionFacts


/**
 * Represents a deadline check entity containing details about a specific data stream upload process.
 *
 * @property dataStreamId Unique identifier of the data stream being monitored.
 * @property dataStreamRoute Specifies the route or path associated with the data stream.
 * @property expectedJurisdictions List of expected jurisdictions in the data stream.
 * @property missingJurisdictions List of expected jurisdictions that are missing in the data stream.
 * @property timestamp The timestamp indicating when the deadline check was performed.
 */
data class DeadlineCheck(
    val dataStreamId: String,
    val dataStreamRoute: String,
    val expectedJurisdictions: List<String>,
    val missingJurisdictions: List<String>,
    val timestamp: String,
    val jurisdictionCounts: Map<String, JurisdictionFacts>
)
