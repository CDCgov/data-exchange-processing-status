package gov.cdc.ocio.database.mongo


/**
 * MongoDB configuration
 *
 * @property connectionString String
 * @property databaseName String
 * @constructor
 */
data class MongoConfiguration(
    val connectionString: String,
    val databaseName: String
)