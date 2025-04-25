package gov.cdc.ocio.messagesystem.plugins

import com.rabbitmq.client.*
import gov.cdc.ocio.messagesystem.MessageProcessorInterface
import gov.cdc.ocio.messagesystem.MessageSystem
import gov.cdc.ocio.messagesystem.config.RabbitMQServiceConfiguration
import gov.cdc.ocio.messagesystem.rabbitmq.RabbitMQMessageSystem
import io.ktor.server.application.*
import io.ktor.server.application.hooks.*
import mu.KotlinLogging
import org.apache.qpid.proton.TimeoutException
import org.koin.java.KoinJavaComponent.getKoin
import java.io.IOException


val RabbitMQPlugin = createApplicationPlugin(
    name = "RabbitMQ",
    configurationPath = "rabbitMQ",
    createConfiguration = ::RabbitMQServiceConfiguration) {

    val logger = KotlinLogging.logger {}

    val queueName = pluginConfig.listenQueueName
    var connection: Connection? = null
    var channel: Channel? = null

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
                    val rabbitMQProcessor = pluginConfig.messageProcessor

                    val message = String(body, Charsets.UTF_8)
                    logger.info("Message received from RabbitMQ queue $queueName with routingKey $routingKey.")
                    runCatching {
                        rabbitMQProcessor.processMessage(message)
                    }.onFailure {
                        logger.error { "Failed to process the incoming message: ${it.localizedMessage}"}
                    }

                    // Acknowledge the message
                    channel.basicAck(deliveryTag, false)
                }
            })
        } catch (e: IOException) {
            logger.error("IOException occurred failed to process message from the queue $e.message")
        }

    }

    on(MonitoringEvent(ApplicationStarted)) {
        logger.info("Application started successfully.")
        try {
            val msgSystem = getKoin().get<MessageSystem>() as RabbitMQMessageSystem
            connection = msgSystem.rabbitMQConnection
            logger.info("Connection to the RabbitMQ server was successfully established")
            channel = connection?.createChannel()
            logger.info("Channel was successfully created.")
        } catch (e: IOException ) {
            logger.error("IOException occurred {}", e.message)
        } catch (e: TimeoutException){
            logger.error("TimeoutException occurred $e.message")
        }
        channel?.let { consumeMessages(it, queueName) }
    }

    on(MonitoringEvent(ApplicationStopped)) { application ->
        logger.info("Application stopped successfully.")
        if (channel != null && connection != null)
            cleanupResourcesAndUnsubscribe(channel!!, connection!!, application)
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
    val logger = KotlinLogging.logger {}

    logger.info("Closing RabbitMQ connection and channel")
    channel.close()
    connection.close()
    application.environment.monitor.unsubscribe(ApplicationStarted) {}
    application.environment.monitor.unsubscribe(ApplicationStopped) {}
}

/**
 * The main application module which runs always
 */
fun Application.rabbitMQModule(messageProcessorImpl: MessageProcessorInterface) {
    install(RabbitMQPlugin) {
        messageProcessor = messageProcessorImpl
    }
}
