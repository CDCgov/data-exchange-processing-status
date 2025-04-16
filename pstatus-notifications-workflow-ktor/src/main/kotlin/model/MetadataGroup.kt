package gov.cdc.ocio.processingnotifications.model

data class MetadataGroup (
    val dataStreamId: String,
    val dataStreamRoute: String,
    val jurisdiction: String,
)