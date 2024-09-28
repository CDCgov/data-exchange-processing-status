package gov.cdc.ocio.database

enum class DatabaseType(val value: String) {
    MONGO("mongo"),
    COUCHBASE("couchbase"),
    COSMOS("cosmos"),
    DYNAMO("dynamo")
}