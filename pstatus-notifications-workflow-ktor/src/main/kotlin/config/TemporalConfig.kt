package gov.cdc.ocio.processingnotifications.config

/**
 * Configuration for the temporal server.
 *
 * @property serviceTarget String
 * @property namespace String
 * @constructor
 */
data class TemporalConfig(
    val serviceTarget: String,
    val namespace: String
)
