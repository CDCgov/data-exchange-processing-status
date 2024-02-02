package gov.cdc.ocio.processingstatusapi.functions

import com.azure.cosmos.models.CosmosQueryRequestOptions
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import com.microsoft.azure.servicebus.QueueClient
import com.microsoft.azure.servicebus.ReceiveMode
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder
import com.microsoft.azure.servicebus.primitives.ServiceBusException
import gov.cdc.ocio.processingstatusapi.cosmos.CosmosClientManager
import gov.cdc.ocio.processingstatusapi.model.CosmosDb
import gov.cdc.ocio.processingstatusapi.model.HealthCheck
import gov.cdc.ocio.processingstatusapi.model.Jaeger
import gov.cdc.ocio.processingstatusapi.model.ServiceBus
import gov.cdc.ocio.processingstatusapi.model.reports.Report
import mu.KotlinLogging
import java.util.*
import kotlin.system.measureTimeMillis

/**
 * Run health checks for the service.
 *
 * @property request HttpRequestMessage<Optional<String>>
 * @property logger (Logger..Logger?)
 * @constructor
 */
class HealthCheckFunction(
    private val request: HttpRequestMessage<Optional<String>>
) {
    val logger = KotlinLogging.logger {}

    fun run(): HttpResponseMessage {
        var cosmosDBHealthy = false
        var serviceBusHealthy = false
        var jaegerHealthy = false
        val cosmosDBHealth = CosmosDb()
        val serviceBusHealth = ServiceBus()
        val jaegerHealth = Jaeger()
        val time = measureTimeMillis {
            try {
                cosmosDBHealthy = isCosmosDBHealthy()
                cosmosDBHealth.status = "UP"
            } catch (ex: Exception) {
                cosmosDBHealth.healthIssues = ex.message
                logger.error("CosmosDB is not healthy: ${ex.message}")
            }

            try {
                serviceBusHealthy = isServiceBusHealthy()
                serviceBusHealth.status = "UP"
            } catch (ex: Exception) {
                serviceBusHealth.healthIssues = ex.message
                logger.error("Azure Service Bus is not healthy: ${ex.message}")
            }

            try {
                jaegerHealthy = isJaegerHealthy()
                if(jaegerHealthy){
                    jaegerHealth.status = "UP"
                }
            } catch (ex: Exception) {
                jaegerHealth.healthIssues = ex.message
                logger.error("Jaeger is not healthy: ${ex.message}")
            }
        }

        val result = HealthCheck().apply {
            status = if (cosmosDBHealthy && serviceBusHealthy && jaegerHealthy) "UP" else "DOWN"
            totalChecksDuration = formatMillisToHMS(time)
            dependencyHealthChecks.add(cosmosDBHealth)
            dependencyHealthChecks.add(serviceBusHealth)
            dependencyHealthChecks.add(jaegerHealth)
        }

        return request
            .createResponseBuilder(HttpStatus.OK)
            .header("Content-Type", "application/json")
            .body(result)
            .build()
    }

    /**
     * Check whether CosmosDB is healthy.
     *
     * @return Boolean
     */
    private fun isCosmosDBHealthy(): Boolean {
        val databaseName = System.getenv("CosmosDbDatabaseName")
        val containerName = System.getenv("CosmosDbContainerName")

        val cosmosDB = CosmosClientManager.getCosmosClient().getDatabase(databaseName)
        val container = cosmosDB.getContainer(containerName)

        val sqlQuery = "select * from $containerName t OFFSET 0 LIMIT 1"
        container.queryItems(
            sqlQuery, CosmosQueryRequestOptions(),
            Report::class.java
        )
        return true
    }

    /**
     * Check whether service bus is healthy.
     *
     * @return Boolean
     */
    @Throws(InterruptedException::class, ServiceBusException::class)
    private fun isServiceBusHealthy(): Boolean {
        val connectionString = System.getenv("ServiceBusConnectionString")
        val queueName = System.getenv("ServiceBusQueueName")
        val queueClient = QueueClient(ConnectionStringBuilder(connectionString, queueName), ReceiveMode.PEEKLOCK)
        queueClient.close()
        return true
    }

    private fun isJaegerHealthy(): Boolean {
        val healthEndPoint = System.getenv("JAEGER_HEALTH_END_POINT")
        logger.info("healthEndPoint: $healthEndPoint")
        val response =  khttp.get(healthEndPoint)
        logger.info("response.statusCode:" + response.statusCode)
        return response.statusCode == HttpStatus.OK.value();
    }

    /**
     * Format the time in milliseconds to 00:00:00.000 format.
     *
     * @param millis Long
     * @return String
     */
    private fun formatMillisToHMS(millis: Long): String {
        val seconds = millis / 1000
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val remainingSeconds = seconds % 60
        val remainingMillis = millis % 1000

        return "%02d:%02d:%02d.%03d".format(hours, minutes, remainingSeconds, remainingMillis / 10)
    }

}