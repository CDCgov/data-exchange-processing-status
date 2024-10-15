package gov.cdc.ocio.eventreadersink.sink

import gov.cdc.ocio.eventreadersink.camel.AzureRoutes
import gov.cdc.ocio.eventreadersink.exceptions.BadServiceException
import gov.cdc.ocio.eventreadersink.exceptions.BadStateException
import gov.cdc.ocio.eventreadersink.exceptions.ConfigurationException
import org.apache.camel.CamelContext
import org.apache.camel.impl.DefaultCamelContext
import org.apache.qpid.jms.JmsConnectionFactory
import org.apache.camel.component.amqp.AMQPComponent
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * AzureSink handles the configuration and setup for sinking messages
 * from an Azure Service Bus topic subscription to Azure Blob Storage.
 */
class AzureSink {

    /**
     * Sinks messages from an Azure Service Bus topic subscription to Blob Storage.
     *
     * This method initializes the Camel context, configures the AMQP component,
     * and adds the necessary routes for processing incoming messages.
     *
     * @param connectionString The connection string for the Azure Service Bus.
     * @param accountName The Azure storage account name.
     * @param accountKey The Azure storage account key.
     * @param containerName The name of the Blob Storage container.
     * @param serviceBusNamespace The namespace for the Azure Service Bus.
     * @param sharedAccessKeyName The shared access key name for the Service Bus.
     * @param sharedAccessKey The shared access key for the Service Bus.
     * @param topicName The name of the Azure Service Bus topic.
     * @param subscriptionName The name of the subscription to the topic.
     * @throws BadServiceException
     */
    @Throws(BadServiceException:: class, Exception:: class)
    fun sinkAsbTopicSubscriptionToBlob(
        connectionString: String,
        accountName: String,
        accountKey: String,
        containerName: String,
        serviceBusNamespace: String,
        sharedAccessKeyName: String,
        sharedAccessKey: String,
        topicName: String,
        subscriptionName: String
    ) {

        try {
            val camelContext: CamelContext = DefaultCamelContext()

            configureAmqpComponent(
                connectionString,
                camelContext,
                serviceBusNamespace,
                sharedAccessKeyName,
                sharedAccessKey
            )
            camelContext.addRoutes(AzureRoutes(topicName, subscriptionName, accountName, accountKey, containerName))
            startCamelContext(camelContext)
        } catch (e: BadServiceException) {
            logger.error("Failed to sink messages from Azure Service Bus to Blob Storage: ${e.message}", e)
            throw BadServiceException("Error initializing AzureSink: ${e.message}")
        } catch (e: Exception) {
            logger.error("Failed to sink messages from Azure Service Bus to Blob Storage: ${e.message}", e)
            throw e
        }
    }

    /**
     * Starts the Camel context and registers a shutdown hook to stop it on exit.
     * @param camelContext The Camel context to start.
     * @throws BadStateException
     */
    @Throws (BadStateException:: class, Exception:: class)
    private fun startCamelContext(camelContext: CamelContext) {
        try {
            camelContext.start()
            Runtime.getRuntime().addShutdownHook(Thread {
                try {
                    camelContext.stop()
                } catch (e: BadStateException) {
                    logger.error("Failed to stop Camel context: ${e.message}", e)
                    throw BadStateException("Error starting Camel context: ${e.message}")
                } catch (e: Exception) {
                    logger.error("Failed to stop Camel context due to an unexpected error: ${e.message}", e)
                    throw e
                }
            })
        } catch (e: BadStateException) {
            logger.error("Failed to stop Camel context: ${e.message}", e)
            throw BadStateException("Error starting Camel context: ${e.message}")
        } catch (e: Exception) {
            logger.error("Failed to start Camel context due to an unexpected error: ${e.message}", e)
            throw e
        }
    }

    /**
     * Configures the AMQP component for the specified Camel context.
     *
     * @param connectionString The connection string for the Azure Service Bus.
     * @param camelContext The Camel context to configure.
     * @param serviceBusHostname The hostname of the Azure Service Bus.
     * @param sharedAccessKeyName The shared access key name for the Service Bus.
     * @param sharedAccessKey The shared access key for the Service Bus.
     * @throws ConfigurationException
     */
    @Throws(ConfigurationException:: class)
    private fun configureAmqpComponent(
        connectionString: String,
        camelContext: CamelContext,
        serviceBusHostname: String,
        sharedAccessKeyName: String,
        sharedAccessKey: String
    ) {
        try {
            val connectionFactory = JmsConnectionFactory().apply {
                remoteURI = "amqps://${connectionString.substringAfter("sb://")}"
                username = sharedAccessKeyName
                password = sharedAccessKey
            }

            val amqpComponent = AMQPComponent().apply {
                setCamelContext(camelContext)
                this.connectionFactory = connectionFactory
            }

            camelContext.addComponent("amqp", amqpComponent)
        } catch (e: ConfigurationException) {
            logger.error("Failed to configure AMQP component: ${e.message}", e)
            throw ConfigurationException("Error configuring AMQP component ${e.message}")
        } catch (e: Exception) {
            logger.error("Failed to configure AMQP component due to an unexpected error: ${e.message}", e)
            throw e
        }
    }
}
