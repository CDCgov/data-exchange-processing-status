package gov.cdc.ocio.processingstatusapi

/**
 * Report manager configuration that will be instantiated as a static (companion) object.
 *
 * @property reportsContainerName String
 * @property partitionKey String
 */
class ReportManagerConfig {
    val reportsContainerName = "Reports"
    private val partitionKey = "/uploadId"
}