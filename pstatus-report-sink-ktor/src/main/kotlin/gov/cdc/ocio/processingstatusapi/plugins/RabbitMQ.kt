package gov.cdc.ocio.processingstatusapi.plugins

import com.rabbitmq.client.*
import gov.cdc.ocio.processingstatusapi.plugins.RMQConstants.DEFAULT_HOST
import gov.cdc.ocio.processingstatusapi.plugins.RMQConstants.DEFAULT_PASSWORD
import gov.cdc.ocio.processingstatusapi.plugins.RMQConstants.DEFAULT_USERNAME
import gov.cdc.ocio.processingstatusapi.plugins.RMQConstants.DEFAULT_VIRTUAL_HOST
import gov.cdc.ocio.processingstatusapi.utils.Helpers
import io.ktor.server.application.*
import io.ktor.server.application.hooks.*
import io.ktor.server.config.*
import org.apache.qpid.proton.TimeoutException
import java.io.IOException

object RMQConstants {
    const val DEFAULT_HOST = "localhost"
    const val DEFAULT_VIRTUAL_HOST = "/"
    const val DEFAULT_USERNAME = "guest"
    const val DEFAULT_PASSWORD = "guest"
}


class RabbitMQUtil(config: ApplicationConfig,configurationPath: String? = null) {
    private val connectionFactory: ConnectionFactory = ConnectionFactory()
    private val configPath= if (configurationPath != null) "$configurationPath." else ""
    val queue = config.tryGetString("${configPath}queue_name") ?: ""

    init {
        connectionFactory.host = config.tryGetString("${configPath}host") ?: DEFAULT_HOST
        connectionFactory.port = config.tryGetString("${configPath}port") ?.toInt()!!
        connectionFactory.virtualHost = config.tryGetString("${configPath}virtual_host") ?: DEFAULT_VIRTUAL_HOST
        connectionFactory.username = config.tryGetString("${configPath}user_name") ?: DEFAULT_USERNAME
        connectionFactory.password = config.tryGetString("${configPath}password") ?: DEFAULT_PASSWORD
        //attempt recovery every 10 seconds
        connectionFactory.setNetworkRecoveryInterval(10000)
    }
    fun getConnectionFactory(): ConnectionFactory {
        return connectionFactory
    }
}

val RabbitMQPlugin = createApplicationPlugin(
    name = "RabbitMQ",
    configurationPath = "rabbitMQ",
    createConfiguration = ::RabbitMQUtil) {

    val factory = pluginConfig.getConnectionFactory()
    val queueName = pluginConfig.queue

    lateinit var connection: Connection
    lateinit var channel: Channel

    try {
        connection = factory.newConnection()
        Helpers.logger.info("Connection to the RabbitMQ server was successfully established")
        channel = connection.createChannel()
        Helpers.logger.info("Channel was successfully created.")
    } catch (e: IOException ) {
        Helpers.logger.error("IOException occurred {}", e.message)
    }catch (e: TimeoutException){
        Helpers.logger.error("TimeoutException occurred $e.message")
    }


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
                    //val contentType = properties.contentType
                    val deliveryTag = envelope.deliveryTag

                    val message = String(body, Charsets.UTF_8)
                    Helpers.logger.info("Message received from RabbitMQ queue $queueName with routingKey $routingKey.")
                    RabbitMQProcessor().validateMessage(message)


                    // Acknowledge the message
                    channel.basicAck(deliveryTag, false)
                }
            })
        }catch (e: IOException){
            Helpers.logger.error("IOException occurred failed to process message from the queue $e.message")
        }

    }

    on(MonitoringEvent(ApplicationStarted)) { application ->
        application.log.info("Application started successfully.")
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

fun Application.rabbitMQModule() {
    install(RabbitMQPlugin)
}

