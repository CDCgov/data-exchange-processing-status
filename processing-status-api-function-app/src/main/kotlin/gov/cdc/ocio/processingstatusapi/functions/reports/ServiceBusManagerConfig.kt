package gov.cdc.ocio.processingstatusapi.functions.reports

import com.azure.messaging.servicebus.ServiceBusClientBuilder
import com.azure.messaging.servicebus.ServiceBusSenderClient

class ServiceBusManagerConfig {

    private val sbConnString: String = System.getenv("ServiceBusConnectionString")
    private val sbQueue: String = System.getenv("ServiceBusReportsQueueName")
    val serviceBusSender : ServiceBusSenderClient = ServiceBusClientBuilder()
        .connectionString(sbConnString)
        .sender()
        .queueName(sbQueue)
        .buildClient()
}