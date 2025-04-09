package gov.cdc.ocio.processingnotifications.workflow.digestcounts

data class Counts(
    val started: Int,
    val completed: Int,
    val failedDelivery: Int,
    val delivered: Int
)