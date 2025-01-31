package gov.cdc.ocio.processingstatusapi.plugins

import com.rabbitmq.client.*
import gov.cdc.ocio.processingstatusapi.messagesystems.MessageSystem
import gov.cdc.ocio.processingstatusapi.messagesystems.RabbitMQMessageSystem
import gov.cdc.ocio.processingstatusapi.utils.SchemaValidation
import io.ktor.server.application.*
import io.ktor.server.application.hooks.*
import io.ktor.server.config.*
import org.apache.qpid.proton.TimeoutException
import org.koin.java.KoinJavaComponent.getKoin
import java.io.IOException


/**
 * The `RabbitMQServiceConfiguration` class configures and initializes `RabbitMQ` connection factory based on settings
 * provided in an `ApplicationConfig`.
 *
 * @param config `ApplicationConfig` containing the configuration settings for RabbitMQ, including connection details.
 * @param configurationPath represents prefix used to locate environment variables specific to RabbitMQ within the
 * configuration.
 */
class RabbitMQServiceConfiguration(config: ApplicationConfig, configurationPath: String? = null) {

    companion object {
        const val DEFAULT_HOST = "localhost"
        const val DEFAULT_VIRTUAL_HOST = "/"
        const val DEFAULT_USERNAME = "guest"
        const val DEFAULT_PASSWORD = "guest"
    }

    private val connectionFactory: ConnectionFactory = ConnectionFactory()
    private val configPath= if (configurationPath != null) "$configurationPath." else ""
    val queue = config.tryGetString("${configPath}queue_name") ?: ""

    init {
        connectionFactory.host = config.tryGetString("${configPath}host") ?: DEFAULT_HOST
        connectionFactory.port = config.tryGetString("${configPath}port") ?.toInt()!!
        connectionFactory.virtualHost = config.tryGetString("${configPath}virtual_host") ?: DEFAULT_VIRTUAL_HOST
        connectionFactory.username = config.tryGetString("${configPath}user_name") ?: DEFAULT_USERNAME
        connectionFactory.password = config.tryGetString("${configPath}password") ?: DEFAULT_PASSWORD

        // Attempt recovery every 10 seconds
        connectionFactory.setNetworkRecoveryInterval(10000)
        connectionFactory.connectionTimeout = 10000 // 10 seconds
    }

    fun getConnectionFactory(): ConnectionFactory {
        return connectionFactory
    }
}

val RabbitMQPlugin = createApplicationPlugin(
    name = "RabbitMQ",
    configurationPath = "rabbitMQ",
    createConfiguration = ::RabbitMQServiceConfiguration) {

    val queueName = pluginConfig.queue
    lateinit var connection: Connection
    lateinit var channel: Channel

    /**
     * consumeMessages function listens to the queue, receives the messages
     * @param channel The RabbitMQ `Channel` used for communicating with the queue.
     * @param queueName The name of the queue to consume messages from.
     * @throws IOException
     */
    fun consumeMessages(channel: Channel, queueName: String) {
        val autoAck = false
        val consumerTag = "myConsumerTag"
        try {
            channel.basicConsume(queueName, autoAck, consumerTag, object : DefaultConsumer(channel) {

                override fun handleDelivery(
                    consumerTag: String,
                    envelope: Envelope,
                    properties: AMQP.BasicProperties,
                    body: ByteArray
                ) {
                    val routingKey = envelope.routingKey
                    val deliveryTag = envelope.deliveryTag
                    val rabbitMQProcessor = RabbitMQProcessor()

                    val message = String(body, Charsets.UTF_8)
                    SchemaValidation.logger.info("Message received from RabbitMQ queue $queueName with routingKey $routingKey.")
                    rabbitMQProcessor.processMessage(message)

                    // Acknowledge the message
                    channel.basicAck(deliveryTag, false)
                }
            })
        } catch (e: IOException) {
            SchemaValidation.logger.error("IOException occurred failed to process message from the queue $e.message")
        }

    }

    on(MonitoringEvent(ApplicationStarted)) { application ->
        application.log.info("Application started successfully.")
        try {
            val msgSystem = getKoin().get<MessageSystem>() as RabbitMQMessageSystem
            connection = msgSystem.rabbitMQConnection
            SchemaValidation.logger.info("Connection to the RabbitMQ server was successfully established")
            channel = connection.createChannel()
            SchemaValidation.logger.info("Channel was successfully created.")
        } catch (e: IOException ) {
            SchemaValidation.logger.error("IOException occurred {}", e.message)
        } catch (e: TimeoutException){
            SchemaValidation.logger.error("TimeoutException occurred $e.message")
        }
        consumeMessages(channel, queueName)
    }

    on(MonitoringEvent(ApplicationStopped)) { application ->
        application.log.info("Application stopped successfully.")
        cleanupResourcesAndUnsubscribe(channel, connection, application)
    }
}


/**
 * We need to clean up the resources and unsubscribe from application life events.
 * @param channel The RabbitMQ `Channel` used for communicating with the queue.
 * @param connection The RabbitMQ `Connection` representing the connection to the RabbitMQ
 * server.
 * @param application The Ktor instance , provides access to the environment monitor used
 * for unsubscribing from events.
 */
private fun cleanupResourcesAndUnsubscribe(channel: Channel, connection: Connection, application: Application) {
    application.log.info("Closing RabbitMQ connection and channel")
    channel.close()
    connection.close()
    application.environment.monitor.unsubscribe(ApplicationStarted){}
    application.environment.monitor.unsubscribe(ApplicationStopped){}
}

/**
 * The main application module which runs always
 */
fun Application.rabbitMQModule() {
    install(RabbitMQPlugin)
}
