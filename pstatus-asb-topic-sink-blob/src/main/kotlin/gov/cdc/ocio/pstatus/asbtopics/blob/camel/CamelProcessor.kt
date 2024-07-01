package gov.cdc.ocio.pstatus.asbtopics.blob.camel

import org.apache.camel.CamelContext
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.impl.DefaultCamelContext
import com.azure.storage.blob.BlobServiceClientBuilder
import com.azure.storage.blob.models.BlobStorageException
import org.apache.camel.component.amqp.AMQPComponent
import org.apache.qpid.jms.JmsConnectionFactory

/**
 * Class which is used for reading messages from service bus topics subscription and uploads to blob storage
 */

class CamelProcessor() {

    /**
     *  Function which is used for reading messages from service bus topics subscription and uploads to blob storage using apache camel route builder
     *  @param connectionString String
     *  @param topicName String
     *  @param subscriptionName String
     *  @param sharedAccessKeyName String
     *  @param sharedAccessKey String
     *  @param accountKey String
     *  @param accountName String
     *  @param containerName String
     */
    fun sinkAsbTopicsToBlob(connectionString:String, topicName:String,subscriptionName:String, sharedAccessKeyName:String, sharedAccessKey:String, accountName:String,accountKey:String,containerName:String) {

        // Initialize the Camel context
        val camelContext: CamelContext = DefaultCamelContext()

        // Configure the JMS component with AMQP over WebSocket
        configureAMQPComponent(camelContext, connectionString, sharedAccessKeyName, sharedAccessKey)

        // Add routes
        camelContext.addRoutes(object : RouteBuilder() {
            override fun configure() {
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

    /**
     * Function which creates a JMSConnectionFactory instance and which is then set as the connection for AMQP component which sets the camel context
     * @param camelContext CamelContext
     * @param connectionString String
     * @param sharedAccessKey String
     * @param sharedAccessKeyName String
     *
     */
    private fun configureAMQPComponent(camelContext: CamelContext, connectionString: String,  sharedAccessKeyName: String, sharedAccessKey: String) {
        // Create the AMQP URI
        val endpoint = connectionString
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
