package gov.cdc.ocio.processingstatusapi.plugins

import com.rabbitmq.client.*
import gov.cdc.ocio.processingstatusapi.plugins.RMQConstants.DEFAULT_HOST
import gov.cdc.ocio.processingstatusapi.plugins.RMQConstants.DEFAULT_PASSWORD
import gov.cdc.ocio.processingstatusapi.plugins.RMQConstants.DEFAULT_PORT
import gov.cdc.ocio.processingstatusapi.plugins.RMQConstants.DEFAULT_USERNAME
import gov.cdc.ocio.processingstatusapi.plugins.RMQConstants.DEFAULT_VIRTUAL_HOST
import io.ktor.server.application.*
import io.ktor.server.application.hooks.*
import io.ktor.server.config.*

object RMQConstants {
    const val DEFAULT_HOST = "localhost"
    const val DEFAULT_PORT = 5672
    const val DEFAULT_VIRTUAL_HOST = "/"
    const val DEFAULT_USERNAME = "guest"
    const val DEFAULT_PASSWORD = "guest"
}

class RabbitMQUtil(config: ApplicationConfig,configurationPath: String? = null) {
    private val connectionFactory: ConnectionFactory = ConnectionFactory()
    private val configPath= if (configurationPath != null) "$configurationPath." else ""
    val queue = config.tryGetString("${configPath}queue_name") ?: ""
    val exchange = config.tryGetString("${configPath}exchange_name") ?: ""
    val routingKey = config.tryGetString("${configPath}routing_key") ?: ""

    init {
        connectionFactory.host = config.tryGetString("${configPath}host") ?: DEFAULT_HOST
        connectionFactory.port = config.tryGetString("${configPath}port") ?.toInt()!!
        connectionFactory.virtualHost = config.tryGetString("${configPath}virtual_host") ?: DEFAULT_VIRTUAL_HOST
        connectionFactory.username = config.tryGetString("${configPath}user_name") ?: DEFAULT_USERNAME
        connectionFactory.password = config.tryGetString("${configPath}password") ?: DEFAULT_PASSWORD
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
    val exchangeName = pluginConfig.exchange
    val routingKeyName = pluginConfig.routingKey

    LOGGER.info("FACTORY ${factory.virtualHost}, port ${factory.port} user ${factory.username}  password ${factory.password} virtual_host ${factory.virtualHost}" )

    val connection by lazy {factory.newConnection()}
    val channel by lazy {connection.createChannel()}

    LOGGER.info("Connection output $connection")
    LOGGER.info ("channel was successfully created $channel")

    fun consumeMessages(channel: Channel, queueName: String) {
        val autoAck = false
        val consumerTag = "myConsumerTag"
       // channel.exchangeDeclare(exchangeName, BuiltinExchangeType.TOPIC, true)

       // channel.queueDeclare(queueName, true, false, false, null)
        LOGGER.info("successfully declared queue $queueName")


        //channel.queueBind(queueName,exchangeName, routingKeyName)
        LOGGER.info("successfully bound queue $queueName")
        channel.basicConsume(queueName, autoAck, consumerTag, object : DefaultConsumer(channel) {

            override fun handleDelivery(
                consumerTag: String,
                envelope: Envelope,
                properties: AMQP.BasicProperties,
                body: ByteArray
            ) {
                val routingKey = envelope.routingKey
                val contentType = properties.contentType
                val deliveryTag = envelope.deliveryTag

                // Process the message components here
                val message = String(body, Charsets.UTF_8)
                println("Received message: $message with routingKey: $routingKey and contentType: $contentType")

                // Acknowledge the message
                channel.basicAck(deliveryTag, false)
            }
        })
    }
    /*
    //@Throws(InterruptedException::class)
    fun startListening() {

        LOGGER.info("connection output $connection")



        LOGGER.info("successfully created channel with connection to the rabbitmq server")

        channel.exchangeDeclare(exchangeName, BuiltinExchangeType.TOPIC, true)

        channel.queueDeclare(queueName, true, false, false, null)
        LOGGER.info("successfully declared queue $queueName")


        channel.queueBind(queueName,exchangeName, routingKeyName)
        LOGGER.info("successfully bind queue to exchange $exchangeName  queueName $queueName and routing key $routingKeyName and ")

        LOGGER.info("Starting RabbitMQ consumer for queue: $queueName")

        channel.basicConsume(queueName, false, { consumerTag, message: Delivery ->
            LOGGER.info("inside basic consume")
            val receivedMessage = String(message.body, Charsets.UTF_8)
            LOGGER.info("Received message {} with consumerTag {} ", receivedMessage, consumerTag)

        }, { consumerTag ->
            LOGGER.warn("error $consumerTag")
        })
    }

     */

    on(MonitoringEvent(ApplicationStarted)) { application ->
        application.log.info("Application started successfully is ready to process messages")
        consumeMessages(channel, queueName)
    }

    on(MonitoringEvent(ApplicationStopped)) { application ->
        application.log.info("Application stopped successfully")
        cleanupResourcesAndUnsubscribe(channel, connection, application)
    }
}


/**
 * We need to clean up the resources and unsubscribe from application life events.
 * During the application's lifecycle, various resources like network connections,
 * file handles, and memory allocations are used. When the application is about to stop
 * it's critical to clean up these resources to avoid potential issues like resource leaks.
 *
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

