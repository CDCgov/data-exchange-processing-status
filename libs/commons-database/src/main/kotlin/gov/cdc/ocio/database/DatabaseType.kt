package gov.cdc.ocio.database

/**
 * Enumeration of all the supported database types for this library.
 *
 * @property value String
 * @constructor
 */
enum class DatabaseType(val value: String) {
    MONGO("mongo"),
    COUCHBASE("couchbase"),
    COSMOS("cosmos"),
    DYNAMO("dynamo")
}