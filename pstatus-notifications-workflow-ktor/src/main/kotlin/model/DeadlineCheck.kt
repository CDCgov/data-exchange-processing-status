package gov.cdc.ocio.processingnotifications.model

import gov.cdc.ocio.processingnotifications.query.DeadlineCheckResults
import gov.cdc.ocio.processingnotifications.workflow.deadlinecheck.JurisdictionFacts


/**
 * Represents a deadline check for a specific data stream, including related results and jurisdiction counts.
 *
 * @property dataStreamId The unique identifier for the data stream being checked.
 * @property dataStreamRoute Indicates the route or path associated with the data stream.
 * @property expectedJurisdictions List of jurisdiction identifiers that are expected to participate.
 * @property deadlineCheckResults Contains the results of the deadline check, including missing and late jurisdictions.
 * @property timestamp The timestamp of when the deadline check was performed.
 * @property jurisdictionCounts A map containing jurisdiction-specific facts, such as count and the last upload time.
 */
data class DeadlineCheck(
    val dataStreamId: String,
    val dataStreamRoute: String,
    val expectedJurisdictions: List<String>,
    val deadlineCheckResults: DeadlineCheckResults,
    val timestamp: String,
    val jurisdictionCounts: Map<String, JurisdictionFacts>
)
