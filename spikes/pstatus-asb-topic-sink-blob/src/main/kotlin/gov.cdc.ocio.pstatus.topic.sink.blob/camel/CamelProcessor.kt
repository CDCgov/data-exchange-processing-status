package gov.cdc.ocio.pstatus.topic.sink.blob.camel

import org.apache.camel.CamelContext
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.impl.DefaultCamelContext
import com.azure.storage.blob.BlobServiceClientBuilder
import com.azure.storage.blob.models.BlobStorageException
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.apache.camel.component.amqp.AMQPComponent
import org.apache.qpid.jms.JmsConnectionFactory

class CamelProcessor {

    fun sinkAsbTopicSubscriptionToBlob(
        connectionString: String, accountName: String, accountKey: String, containerName: String,
        serviceBusNamespace: String, sharedAccessKeyName: String, sharedAccessKey: String, topicName: String, subscriptionName: String
    ) {
        // Initialize the Camel context
        val camelContext: CamelContext = DefaultCamelContext()

        // Configure the JMS component with AMQP over WebSocket
        configureAMQPComponent(connectionString,camelContext, serviceBusNamespace, sharedAccessKeyName, sharedAccessKey)

        // Add routes
        camelContext.addRoutes(object : RouteBuilder() {
            override fun configure() {
                //  from("amqp:queue:$queueName")
                from("amqp:queue:$topicName/subscriptions/$subscriptionName")
                    .process { exchange ->
                        val message = exchange.getIn().getBody(String::class.java)
                        exchange.message.setBody(message.toByteArray(Charsets.UTF_8))

                        val blobClient = BlobServiceClientBuilder()
                            .connectionString("DefaultEndpointsProtocol=https;AccountName=$accountName;AccountKey=$accountKey;EndpointSuffix=core.windows.net")
                            .buildClient()
                            .getBlobContainerClient(containerName)
                            .getBlobClient("message-${System.currentTimeMillis()}.json")

                        try {
                            blobClient.upload(message.byteInputStream(), message.length.toLong(), true)
                        } catch (e: BlobStorageException) {
                            log.error("Error uploading to Blob Storage", e)
                        }
                    }

            }
        })
        // Start the Camel context
        camelContext.start()
        Runtime.getRuntime().addShutdownHook(Thread { camelContext.stop() })
    }

    private fun configureAMQPComponent(
        connectionString: String,
        camelContext: CamelContext,
        serviceBusHostname: String,
        sharedAccessKeyName: String,
        sharedAccessKey: String
    ) {
        // Create the AMQP URI
       // val endpoint ="Endpoint=sb://ocio-ede-dev-processingstatus.servicebus.windows.net/;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=PCmVAxYcWtdtHbcMEAXhW1yZNQeDd4jXe+ASbIXaG3A="
        val endpoint =connectionString
        val connectionFactory = JmsConnectionFactory()
        connectionFactory.remoteURI = "amqps://${endpoint.substringAfter("sb://")}"
        connectionFactory.username = sharedAccessKeyName
        connectionFactory.password = sharedAccessKey
        // Create the AMQP component
        val amqpComponent = AMQPComponent()
        amqpComponent.setCamelContext(camelContext)
        amqpComponent.connectionFactory =connectionFactory
        // Register the component
        camelContext.addComponent("amqp", amqpComponent)
    }
}
