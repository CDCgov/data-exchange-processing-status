package gov.cdc.ocio.pstatus.topic.sink.blob.camel

import org.apache.camel.CamelContext
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.impl.DefaultCamelContext
import com.azure.storage.blob.BlobServiceClientBuilder
import com.azure.storage.blob.models.BlobStorageException
import gov.cdc.ocio.pstatus.topic.sink.blob.aws.AwsManager
import gov.cdc.ocio.pstatus.topic.sink.model.CloudConfig
import org.apache.camel.component.amqp.AMQPComponent
import org.apache.qpid.jms.JmsConnectionFactory

class CamelProcessor {

    /*
    * Sink messages from the given cloud provider messaging system into the respective storage
    * AWS - Sink messages from SNS/SQS Queue into the S3 bucket
    * Azure - Sink messages delivered though Azure Service Bus into the Azure Container
    * */
    fun sinkMessageToStorage(cloudConfig: CloudConfig){

        when (cloudConfig.provider) {
            "aws" -> {
               sinkSQSTopicSubscriptionToS3(cloudConfig)
            }
            "azure" -> {
                sinkAsbTopicSubscriptionToBlob(cloudConfig.connectionString.toString(),
                    cloudConfig.storageAccountName.toString(),
                    cloudConfig.storageAccountKey.toString(),
                    cloudConfig.containerName.toString(),
                    cloudConfig.namespace.toString(),cloudConfig.sharedAccessKeyName.toString(),
                    cloudConfig.sharedAccessKey.toString(), cloudConfig.topicName.toString(), cloudConfig.subscriptionName.toString())
            }
            else -> throw IllegalArgumentException("Unsupported cloud provider")
        }

    }


    /*
        * Sink messages from the given cloud provider messaging system into the respective storage
        * AWS - Sink messages from SNS/SQS Queue into the S3 bucket
    * */
    private fun sinkSQSTopicSubscriptionToS3(cloudConfig: CloudConfig){

        // Initialize the Camel context
        val camelContext: CamelContext = DefaultCamelContext()

        //Configure AWS Components
        // Configure SQS Component
        val sqsComponent = AwsManager().configureAwsSQSComponent(cloudConfig.awsAccessKeyId.toString(), cloudConfig.awsSecretAccessKey.toString(), cloudConfig.awsSqsRegion.toString())
        camelContext.addComponent("aws2-sqs", sqsComponent)

        // Configure S3 Component
        val s3Component = AwsManager().configureAwsS3Component(cloudConfig.awsAccessKeyId.toString(), cloudConfig.awsSecretAccessKey.toString(), cloudConfig.awsS3Region.toString())
        camelContext.addComponent("aws2-s3", s3Component)

        // Add routes
        camelContext.addRoutes(object : RouteBuilder() {
            override fun configure() {
                from("aws2-sqs://${cloudConfig.awsSqsQueueName}")
                    .setHeader("CamelAwsS3Key", simple("message-${System.currentTimeMillis()}.txt")) // Set S3 key dynamically
                    .to("aws2-s3://${cloudConfig.awsS3BucketName}?accessKey=${cloudConfig.awsAccessKeyId}&secretKey=${cloudConfig.awsSecretAccessKey}&region=${cloudConfig.awsS3Region}")
            }
        })

        // Start the Camel context
        camelContext.start()
        Runtime.getRuntime().addShutdownHook(Thread { camelContext.stop() })
    }


    /*
        * Sink messages from the given cloud provider messaging system into the respective storage
        * Azure - Sink messages delivered though Azure Service Bus into the Azure Container
    * */
    private fun sinkAsbTopicSubscriptionToBlob(
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
        val endpoint ="Endpoint=sb://ocio-ede-dev-processingstatus.servicebus.windows.net/;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=PCmVAxYcWtdtHbcMEAXhW1yZNQeDd4jXe+ASbIXaG3A="
       // val endpoint =connectionString
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
