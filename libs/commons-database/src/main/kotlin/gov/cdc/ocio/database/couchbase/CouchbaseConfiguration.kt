package gov.cdc.ocio.database.couchbase


/**
 * Couchbase configuration
 *
 * @property connectionString String
 * @property username String
 * @property password String
 * @constructor
 */
data class CouchbaseConfiguration(
    val connectionString: String,
    val username: String,
    val password: String
)