package gov.cdc.ocio.processingnotifications.cosmos

/**
 * CosmosDB client configuration
 *
 * @property uri String
 * @property authKey String
 * @constructor
 */
data class CosmosConfiguration(val uri: String, val authKey: String)