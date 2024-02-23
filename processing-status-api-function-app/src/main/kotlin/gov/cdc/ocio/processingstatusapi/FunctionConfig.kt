package gov.cdc.ocio.processingstatusapi

import com.azure.messaging.servicebus.ServiceBusClientBuilder
import com.azure.messaging.servicebus.ServiceBusSenderClient

class FunctionConfig {
    val sbConnString = System.getenv("ServiceBusConnectionString")
    val sbQueue = System.getenv("ServiceBusReportsQueueName")
    val serviceBusSender : ServiceBusSenderClient = ServiceBusClientBuilder()
        .connectionString(sbConnString)
        .sender()
        .queueName(sbQueue)
        .buildClient()
}
