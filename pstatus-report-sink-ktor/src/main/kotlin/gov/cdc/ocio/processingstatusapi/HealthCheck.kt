package gov.cdc.ocio.processingstatusapi

import aws.sdk.kotlin.services.sqs.SqsClient
import com.azure.core.exception.ResourceNotFoundException
import com.azure.messaging.servicebus.ServiceBusException
import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClientBuilder
import com.rabbitmq.client.Connection
import gov.cdc.ocio.database.cosmos.CosmosClientManager
import gov.cdc.ocio.database.cosmos.CosmosConfiguration
import gov.cdc.ocio.processingstatusapi.exceptions.BadStateException
import gov.cdc.ocio.processingstatusapi.plugins.AWSSQServiceConfiguration
import gov.cdc.ocio.processingstatusapi.plugins.AzureServiceBusConfiguration
import gov.cdc.ocio.processingstatusapi.plugins.RabbitMQServiceConfiguration
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.system.measureTimeMillis


/**
 * Abstract class used for modeling the health issues of an individual service.
 *
 * @property status String
 * @property healthIssues String?
 * @property service String
 */
abstract class HealthCheckSystem {

    var status: String = "DOWN"

    private var healthIssues: String? = ""

    open val service: String = ""
}

/**
 * Concrete implementation of the cosmosdb service health check.
 *
 * @property service String
 */
class HealthCheckCosmosDb: HealthCheckSystem() {
    override val service = "Cosmos DB"
}

/**
 * Concrete implementation of the Azure Service Bus health check.
 *
 * @property service String
 */
class HealthCheckServiceBus: HealthCheckSystem() {
    override val service: String = "Azure Service Bus"
}
/**
 * Concrete implementation of the RabbitMQ health check.
 *
 * @property service String
 */
class HealthCheckRabbitMQ: HealthCheckSystem() {
    override val service: String = "RabbitMQ"
}
/**
 * Concrete implementation of the AWS SQS health check.
 *
 * @property service String
 */
class HealthCheckAWSSQS: HealthCheckSystem() {
    override val service: String = "AWS SQS"
}


/**
 * Concrete implementation of the Unsupported message system
 */
class HealthCheckUnsupportedMessageSystem: HealthCheckSystem() {
    override  val service: String = "Messaging Service"
}

/**
 * Run health checks for the service.
 *
 * @property status String?
 * @property totalChecksDuration String?
 * @property dependencyHealthChecks MutableList<HealthCheckSystem>
 */
class HealthCheck {

    companion object{
        const val STATUS_UP = "UP"
        const val STATUS_DOWN = "DOWN"
        const val STATUS_UNSUPPORTED = "UNSUPPORTED"
    }

    var status: String = STATUS_DOWN

    var totalChecksDuration: String? = null

    var dependencyHealthChecks = mutableListOf<HealthCheckSystem>()
}

/**
 * Service for querying the health of the report-sink service and its dependencies.
 *
 * @property logger KLogger
 * @property cosmosConfiguration CosmosConfiguration
 * @property azureServiceBusConfiguration AzureServiceBusConfiguration
 * @property rabbitMQServiceConfiguration RabbitMQServiceConfiguration
 * @property awsSqsServiceConfiguration AWSSQServiceConfiguration
 */

class HealthQueryService: KoinComponent {

    private val logger = KotlinLogging.logger {}

    private val cosmosConfiguration by inject<CosmosConfiguration>()

    private val azureServiceBusConfiguration by inject<AzureServiceBusConfiguration>()

    private val rabbitMQServiceConfiguration by inject<RabbitMQServiceConfiguration>()

    private val awsSqsServiceConfiguration by inject<AWSSQServiceConfiguration>()

    private val msgType: String by inject()


    /**
     * Returns a HealthCheck object with the overall health of the report-sink service and its dependencies.
     *
     * @return HealthCheck
     */
    fun getHealth(): HealthCheck {
        val cosmosDBHealth = HealthCheckCosmosDb()
        var rabbitMQHealth: HealthCheckRabbitMQ? = null
        var serviceBusHealth: HealthCheckServiceBus? = null
        var awsSQSHealth:HealthCheckAWSSQS? = null
        var unsupportedMessageSystem: HealthCheckUnsupportedMessageSystem? = null

        val time = measureTimeMillis {
            checkCosmosDBHealth(cosmosDBHealth)
            // selectively check health of the messaging service based on msgType
            when (msgType) {
                MessageSystem.AZURE_SERVICE_BUS.toString() -> {
                   serviceBusHealth = checkAzureServiceBusHealth()
                }

                MessageSystem.RABBITMQ.toString() -> {
                    rabbitMQHealth = checkRabbitMQHealth()
                }

                MessageSystem.AWS.toString() -> {
                    awsSQSHealth = checkAWSSQSHealth()
                }

                else -> {
                    unsupportedMessageSystem = checkUnsupportedMessageSystem()
                }
            }
        }
        return compileHealthChecks(cosmosDBHealth, serviceBusHealth, rabbitMQHealth, awsSQSHealth, unsupportedMessageSystem,time)
    }

    /**
     * Check whether CosmosDB is healthy.
     *
     * @param config CosmosConfiguration
     * @return Boolean
     */
    private fun isCosmosDBHealthy(config: CosmosConfiguration): Boolean {
        return if (CosmosClientManager.getCosmosClient(config.uri, config.authKey) == null)
            throw Exception("Failed to establish a CosmosDB client.")
        else
            true
    }

    /**
     * Checks and sets cosmosDBHealth status
     *
     * @param cosmosDBHealth The health check object for CosmosDB to update the status
     */
    private fun checkCosmosDBHealth(cosmosDBHealth: HealthCheckCosmosDb){
        try {
            if (isCosmosDBHealthy(cosmosConfiguration)) {
                cosmosDBHealth.status = HealthCheck.STATUS_UP
            }
        }catch (ex: Exception){
            logger.error("Cosmos DB is not healthy $ex.message")
        }
    }

    /**
     * Checks and sets azureServiceBusHealth status
     *
     * @return HealthCheckServiceBus object with updated status
     */
    private fun checkAzureServiceBusHealth():HealthCheckServiceBus{
        val serviceBusHealth = HealthCheckServiceBus()
        try {
            if (isServiceBusHealthy(azureServiceBusConfiguration)) {
                serviceBusHealth.status = HealthCheck.STATUS_UP
            }
        }catch (ex: Exception){
            logger.error("Azure Service Bus is not healthy $ex.message")
        }
        return serviceBusHealth
    }

    /**
     * Checks and sets rabbitMQHealth status
     *
     * @return HealthCheckRabbitMQ object with updated status
     */
    private fun checkRabbitMQHealth():HealthCheckRabbitMQ {
        val rabbitMQHealth = HealthCheckRabbitMQ()
        try {
            if (isRabbitMQHealthy(rabbitMQServiceConfiguration)) {
                rabbitMQHealth.status = HealthCheck.STATUS_UP
            }
        }catch (ex: Exception){
            logger.error("RabbitMQ is not healthy $ex.message")
        }
        return rabbitMQHealth
    }

    /**
     * Checks and sets AWSSQSHealth status
     *
     * @return HealthCheckAWSSQS object with updated status
     */
    private fun checkAWSSQSHealth(): HealthCheckAWSSQS {
        val awSSQSHealth = HealthCheckAWSSQS()
        try {
            if (isAWSSQSHealthy(awsSqsServiceConfiguration)) {
                awSSQSHealth.status = HealthCheck.STATUS_UP
            }

        }catch (ex: Exception){
            logger.error("AWS SQS is not healthy $ex.message")
        }
        return awSSQSHealth
    }

    /**
     * Creates a `HealthCheckUnsupportedMessageSystem` object indicating that the provided message system is unsupported.
     * This function is used to handle cases where unsupported `MSG_SYSTEM` is configured
     *
     * @return HealthCheckUnsupportedMessageSystem object with status and service for unsupported message service
     */
    private fun checkUnsupportedMessageSystem(): HealthCheckUnsupportedMessageSystem {
        val unsupportedMessageSystem = HealthCheckUnsupportedMessageSystem()
        unsupportedMessageSystem.status = HealthCheck.STATUS_UNSUPPORTED
        return unsupportedMessageSystem
    }

    /**
     * Compiles health checks for supported services
     * @param cosmosDBHealth Health check status for Cosmos DB
     * @param azureServiceBusHealth Health check status for Azure Service Bus Health
     * @param rabbitMQHealth Health check status for RabbitMQ health
     * @param unsupportedMessageSystem Health check status for unsupported message system
     * @param totalTime Total duration in milliseconds it took to retrieve health check statuses
     * @return HealthCheck compiled Health check object with aggregated health check results
     */
    private fun compileHealthChecks(cosmosDBHealth: HealthCheckCosmosDb,
                                    azureServiceBusHealth: HealthCheckServiceBus?,
                                    rabbitMQHealth: HealthCheckRabbitMQ?,
                                    awsSQSHealth: HealthCheckAWSSQS?,
                                    unsupportedMessageSystem: HealthCheckUnsupportedMessageSystem?,
                                    totalTime:Long):HealthCheck{
        return HealthCheck().apply {
            status = if (cosmosDBHealth.status == HealthCheck.STATUS_UP && (azureServiceBusHealth?.status == HealthCheck.STATUS_UP || rabbitMQHealth?.status == HealthCheck.STATUS_UP || awsSQSHealth?.status == HealthCheck.STATUS_UP)) HealthCheck.STATUS_UP else HealthCheck.STATUS_DOWN
            totalChecksDuration = formatMillisToHMS(totalTime)
            dependencyHealthChecks.add(cosmosDBHealth)
            azureServiceBusHealth?.let { dependencyHealthChecks.add(it) }
            rabbitMQHealth?.let { dependencyHealthChecks.add(it) }
            awsSQSHealth?.let { dependencyHealthChecks.add(it) }
            unsupportedMessageSystem?.let { dependencyHealthChecks.add(it) }
        }
    }

    /**
     * Check whether service bus is healthy.
     *
     * @return Boolean
     */
    @Throws(ResourceNotFoundException::class, ServiceBusException::class)
    private fun isServiceBusHealthy(config: AzureServiceBusConfiguration): Boolean {

        val adminClient = ServiceBusAdministrationClientBuilder()
            .connectionString(config.connectionString)
            .buildClient()

        // Get the properties of the topic to check the connection
        adminClient.getTopic(config.topicName)

        return true
    }

    /**
     * Check whether rabbitMQ is healthy.
     *
     * @return Boolean
     */
    @Throws(BadStateException::class)
    private fun isRabbitMQHealthy(config: RabbitMQServiceConfiguration): Boolean {
        var rabbitMQConnection: Connection? = null
        return try {
            rabbitMQConnection = config.getConnectionFactory().newConnection()
            rabbitMQConnection.isOpen
        }catch (e: Exception) {
            throw Exception("Failed to establish connection to RabbitMQ server.")
        } finally {
            rabbitMQConnection?.close()
        }
    }

    /**
     * Check whether AWS SQS is healthy.
     *
     * @return Boolean
     */
    @Throws(BadStateException::class)
    private  fun isAWSSQSHealthy(config: AWSSQServiceConfiguration): Boolean {
        val sqsClient: SqsClient?
        return try {
           sqsClient = config.createSQSClient()
            sqsClient.close()
            true
        }catch (e: Exception){
            throw Exception("Failed to establish connection to AWS SQS service.")
        }
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