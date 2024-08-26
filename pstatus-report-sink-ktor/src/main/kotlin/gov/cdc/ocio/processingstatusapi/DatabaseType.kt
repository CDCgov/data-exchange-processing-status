package gov.cdc.ocio.processingstatusapi

enum class DatabaseType(val value: String) {
    MONGO("mongo"),
    COUCHBASE("couchbase"),
    COSMOS("cosmos")
}