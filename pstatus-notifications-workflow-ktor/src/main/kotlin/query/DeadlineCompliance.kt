package gov.cdc.ocio.processingnotifications.query


/**
 * Represents the result of a deadline compliance check for a specific data stream or route.
 *
 * This data class holds information about jurisdictions that are either missing
 * or have failed to report within the expected deadline.
 *
 * @property missingJurisdictions A list of jurisdictions that did not produce any reports within the expected period.
 * @property lateJurisdictions A list of jurisdictions that submitted reports after the specified deadline.
 */
data class DeadlineCompliance(
    val missingJurisdictions: List<String>,
    val lateJurisdictions: List<String>
)