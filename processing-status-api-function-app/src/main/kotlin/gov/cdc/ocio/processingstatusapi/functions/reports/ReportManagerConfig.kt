package gov.cdc.ocio.processingstatusapi.functions.reports

import com.azure.messaging.servicebus.ServiceBusClientBuilder
import com.azure.messaging.servicebus.ServiceBusSenderClient
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

    private val sbConnString: String = System.getenv("ServiceBusConnectionString")
    private val sbQueue: String = System.getenv("ServiceBusReportsQueueName")
    val serviceBusSender : ServiceBusSenderClient = ServiceBusClientBuilder()
        .connectionString(sbConnString)
        .sender()
        .queueName(sbQueue)
        .buildClient()
}