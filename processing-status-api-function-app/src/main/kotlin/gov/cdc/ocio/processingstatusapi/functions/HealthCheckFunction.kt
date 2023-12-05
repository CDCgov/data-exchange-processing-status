package gov.cdc.ocio.processingstatusapi.functions

import com.azure.cosmos.models.CosmosQueryRequestOptions
import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import com.microsoft.azure.servicebus.QueueClient
import com.microsoft.azure.servicebus.ReceiveMode
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder
import gov.cdc.ocio.cosmossync.cosmos.CosmosClientManager.Companion.getCosmosClient
import gov.cdc.ocio.processingstatusapi.model.CosmosDb
import gov.cdc.ocio.processingstatusapi.model.HealthCheck
import gov.cdc.ocio.processingstatusapi.model.Report
import gov.cdc.ocio.processingstatusapi.model.ServiceBus
import java.util.*
import kotlin.system.measureTimeMillis

class HealthCheckFunction {

    fun run(
        request: HttpRequestMessage<Optional<String>>,
        context: ExecutionContext,
    ): HttpResponseMessage {
        val logger = context.logger
        val result = HealthCheck()
        var cosmosDBHealthy = false;
        var serviceBusHealthy = false;
        var cosmosDBHealth = CosmosDb()
        var serviceBusHealth = ServiceBus()
        val time = measureTimeMillis {
            try {
                cosmosDBHealthy = isCosmosDBHealthy()
                cosmosDBHealth.status = "UP"
            } catch (ex: Exception) {
                cosmosDBHealth.health_issues(ex.message)
                logger.warning("CosmosDB is not healthy: ${ex.message}")
            }

            try {
                serviceBusHealthy = isServiceBusHealthy()
                serviceBusHealth.status = "UP"
            } catch (ex: Exception) {
                serviceBusHealth.health_issues(ex.message)
                logger.warning("Azure Service Bus is not healthy: ${ex.message}")
            }
        }

        if(cosmosDBHealthy && serviceBusHealthy){
            result.status = "UP"
        }

        result.total_checks_duration(formatMillisToHMS(time))
        result.dependency_health_checks.add(cosmosDBHealth)
        result.dependency_health_checks.add(serviceBusHealth)
        return request
            .createResponseBuilder(HttpStatus.OK)
            .header("Content-Type", "application/json")
            .body(result)
            .build()
    }

    private fun isCosmosDBHealthy(): Boolean {
        val databaseName = System.getenv("CosmosDbDatabaseName")
        val containerName = System.getenv("CosmosDbContainerName")

        val cosmosDB = getCosmosClient().getDatabase(databaseName)
        val container = cosmosDB.getContainer(containerName)

        val sqlQuery = "select * from $containerName t OFFSET 0 LIMIT 1"
        val items = container.queryItems(
            sqlQuery, CosmosQueryRequestOptions(),
            Report::class.java
        )
        return true
    }

    private fun isServiceBusHealthy():Boolean {
        val connectionString = System.getenv("ServiceBusConnectionString")
        val queueName = System.getenv("ServiceBusQueueName")
        val queueClient = QueueClient(ConnectionStringBuilder(connectionString, queueName), ReceiveMode.PEEKLOCK)
        queueClient.close()
        return true;
    }

    fun formatMillisToHMS(millis: Long): String {
        val seconds = millis / 1000
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val remainingSeconds = seconds % 60
        val remainingMillis = millis % 1000

        return "%02d:%02d:%02d.%02d".format(hours, minutes, remainingSeconds, remainingMillis / 10)
    }


}