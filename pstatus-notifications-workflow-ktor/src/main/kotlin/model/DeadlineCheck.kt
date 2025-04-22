package gov.cdc.ocio.processingnotifications.model

data class DeadlineCheck(
    val dataStreamId: String,
    val jurisdiction: String,
    val timestamp: String
)
