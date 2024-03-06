package gov.cdc.ocio.processingstatusapi.integration

import com.azure.messaging.servicebus.ServiceBusClientBuilder
import com.azure.messaging.servicebus.ServiceBusMessage
import com.azure.messaging.servicebus.ServiceBusSenderClient
import com.google.gson.Gson

import gov.cdc.dex.azure.cosmos.CosmosClient

import org.slf4j.LoggerFactory
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL


data class ResponseObject(val statusCode: Int, val responseBody: String?)

class IntegrationTest {

    companion object {
        private var logger = LoggerFactory.getLogger(Function::class.java.simpleName)

        private val serviceBusConnectionString =  System.getenv("SERVICE_BUS_CONNECTION_STRING")
        private val cosmosDBName = System.getenv("COSMOS_DB_NAME")
        private val cosmosDBContainer =  System.getenv("COSMOS_DB_CONTAINER_NAME")
        private val cosmosDBEndpoint =  System.getenv("COSMOS_DB_ENDPOINT")
        private val cosmosDBKey =  System.getenv("COSMOS_DB_KEY")
        private val cosmosDBPartitionKey =  System.getenv("COSMOS_DB_PARTITION_KEY")


        const val QUEUE_NAME = "processing-status-cosmos-db-queue"
        const val DEX_PS_API_INTEGRATION_TEST_CMD = "dex::ps-api-integration-test"
    }

    fun serviceBusSenderClient(report: String):ServiceBusSenderClient?{
        var senderClient: ServiceBusSenderClient? = null
        try {
            senderClient = ServiceBusClientBuilder()
                .connectionString(serviceBusConnectionString)
                .fullyQualifiedNamespace("ocio-ede-tst-processingstatus")
                .sender()
                .queueName(QUEUE_NAME)
                .buildClient()
            val serviceBusMessage = ServiceBusMessage(report)

            senderClient.sendMessage(serviceBusMessage)
            logger.info("$DEX_PS_API_INTEGRATION_TEST_CMD Message was successfully sent")
        }catch (e:Exception){
            logger.error("$DEX_PS_API_INTEGRATION_TEST_CMD an error occurred while creating service bus sender client: ${e.message}")
        }

        return senderClient


    }

    fun sendReportToProcessingStatusAPI(urlString:String, requestBody:String): ResponseObject {
        var responseCode = 0
        var responseBody: String? = null
        try{
            val processingStatusAPIBaseURL = URL(urlString)
            val connectionToProcessingStatusAPI = processingStatusAPIBaseURL.openConnection() as HttpURLConnection
            connectionToProcessingStatusAPI.requestMethod = "POST"

            connectionToProcessingStatusAPI.setRequestProperty("Content-Type", "application/json")

            connectionToProcessingStatusAPI.doOutput = true
            connectionToProcessingStatusAPI.doInput = true

            val requestObject = requestBody.trimIndent()

            val outputStream = OutputStreamWriter(connectionToProcessingStatusAPI.outputStream)
            outputStream.write(requestObject)
            outputStream.flush()

            responseCode = connectionToProcessingStatusAPI.responseCode

            responseBody = if (responseCode  == HttpURLConnection.HTTP_OK){
                val responseStream = connectionToProcessingStatusAPI.inputStream
                responseStream.bufferedReader().use {it.readText()}
                //return ResponseObject(responseCode, responseBody)
            } else{
                val errorStream = connectionToProcessingStatusAPI.errorStream
                errorStream.bufferedReader().use {it.readText()}
                //return ResponseObject(responseCode,responseBody)
            }
            connectionToProcessingStatusAPI.disconnect()

        }catch (e:Exception){
            logger.error("$DEX_PS_API_INTEGRATION_TEST_CMD  Error occurred while sending report to Processing Status API: ${e.message}")
        }

        return  ResponseObject(responseCode,responseBody)
    }


    fun queryCosmosDB(uploadID: String, fieldToQuery:String): String {
        var payloadAsJson = ""
        try {
            val cosmosDBClient by lazy {
                CosmosClient(
                    cosmosDBName,
                    cosmosDBContainer,
                    cosmosDBEndpoint,
                    cosmosDBKey,
                    cosmosDBPartitionKey
                )
            }

            val queryCosmosDBToRetrievePayload = "SELECT * FROM c WHERE c.$fieldToQuery = \"$uploadID\""
            val payLoad = cosmosDBClient.sqlReadItems(queryCosmosDBToRetrievePayload, Map::class.java).blockLast()
            payloadAsJson = Gson().toJson(payLoad)
            return payloadAsJson

        }catch (e: Exception) {
            logger.error("$DEX_PS_API_INTEGRATION_TEST_CMD  Error occurred while querying Cosmos DB: ${e.message}")

        }

    return payloadAsJson
    }
}