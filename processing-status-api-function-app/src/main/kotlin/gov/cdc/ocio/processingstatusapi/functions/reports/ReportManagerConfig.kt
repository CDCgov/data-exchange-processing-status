package gov.cdc.ocio.processingstatusapi.functions.reports

import gov.cdc.ocio.processingstatusapi.cosmos.CosmosContainerManager

/**
 * Report manager configuration that will be instantiated as a static (companion) object.
 *
 * @property reportsContainerName String
 * @property partitionKey String
 * @property reportsContainer CosmosContainer?
 */
class ReportManagerConfig {
    val reportsContainerName = "Reports"
    private val partitionKey = "/uploadId"
    val reportsContainer = CosmosContainerManager.initDatabaseContainer(reportsContainerName, partitionKey)
}