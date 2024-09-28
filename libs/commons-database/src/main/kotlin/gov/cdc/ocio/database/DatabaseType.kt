package gov.cdc.ocio.gov.cdc.ocio.database.cosmos

enum class DatabaseType(val value: String) {
    MONGO("mongo"),
    COUCHBASE("couchbase"),
    COSMOS("cosmos"),
    DYNAMO("dynamo")
}