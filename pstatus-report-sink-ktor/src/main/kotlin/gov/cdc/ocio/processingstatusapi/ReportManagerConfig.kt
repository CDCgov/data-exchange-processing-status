package gov.cdc.ocio.processingstatusapi

/**
 * Report manager configuration that will be instantiated as a static (companion) object.
 *
 * @property reportsContainerName String
 * @property partitionKey String
 */
class ReportManagerConfig {
    val reportsContainerName = "Reports"
    val reportsDeadLetterContainerName = "Reports-DeadLetter"
    private val partitionKey = "/uploadId"
}