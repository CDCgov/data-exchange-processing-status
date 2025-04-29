package gov.cdc.ocio.processingnotifications.model


/**
 * Represents a deadline check entity containing details about a specific data stream upload process.
 *
 * @property dataStreamId Unique identifier of the data stream being monitored.
 * @property dataStreamRoute Specifies the route or path associated with the data stream.
 * @property missingJurisdictions List of expected jurisdictions that are missing in the data stream.
 * @property timestamp The timestamp indicating when the deadline check was performed.
 */
data class DeadlineCheck(
    val dataStreamId: String,
    val dataStreamRoute: String,
    val missingJurisdictions: List<String>,
    val timestamp: String
)
