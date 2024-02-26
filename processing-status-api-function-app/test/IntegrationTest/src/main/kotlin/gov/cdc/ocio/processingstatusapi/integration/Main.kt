package gov.cdc.ocio.processingstatusapi.integration

import com.azure.messaging.servicebus.ServiceBusClientBuilder
import com.azure.messaging.servicebus.ServiceBusMessage
import com.azure.messaging.servicebus.ServiceBusSenderClient
import org.slf4j.LoggerFactory
import java.util.*


class IntegrationTest {

    companion object {
        private var logger = LoggerFactory.getLogger(Function::class.java.simpleName)
        private val uuid = UUID.randomUUID()
        private val serviceBusConnectionString =  System.getenv("SERVICE_BUS_CONNECTION_STRING")
        private val processingStatusAPIBaseURL =  System.getenv("PROCESSING_STATUS_API_BASE_URL")
        const val QUEUE_NAME = "processing-status-cosmos-db-queue"
        const val DEX_PS_API_INTEGRATION_TEST_CMD = "dex::ps-api-integration-test"
    }

    fun serviceBusSenderClient():ServiceBusSenderClient?{
        var senderClient: ServiceBusSenderClient? = null
        try {
            senderClient = ServiceBusClientBuilder()
                .connectionString(serviceBusConnectionString)
                .sender()
                .queueName(QUEUE_NAME)
                .buildClient()
        }catch (e:Exception){
            logger.error("$DEX_PS_API_INTEGRATION_TEST_CMD an error occurred while creating service bus sender client: ${e.message}")
        }

        return senderClient


    }

    fun sendReportToProcessingStatusAPI(){

    }


    fun queryCosmosDB(){

    }




}
fun main(args: Array<String>) {
    println("Hello World!")

    // Try adding program arguments via Run/Debug configuration.
    // Learn more about running applications: https://www.jetbrains.com/help/idea/running-applications.html.
    println("Program arguments: ${args.joinToString()}")
}