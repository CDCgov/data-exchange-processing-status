package gov.cdc.ocio.reader.sink.camel

import com.azure.storage.blob.BlobServiceClientBuilder
import com.azure.storage.blob.models.BlobStorageException
import com.azure.storage.common.StorageSharedKeyCredential
import gov.cdc.ocio.reader.sink.aws.AwsManager
import gov.cdc.ocio.reader.sink.model.CloudConfig
import org.apache.camel.CamelContext
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.component.amqp.AMQPComponent
import org.apache.camel.component.aws2.s3.AWS2S3Component
import org.apache.camel.component.aws2.s3.AWS2S3Configuration
import org.apache.camel.impl.DefaultCamelContext
import org.apache.qpid.jms.JmsConnectionFactory
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import java.net.URI

class CamelProcessor {
    /*
     * Sink messages from the given cloud provider messaging system into the respective storage
     * AWS - Sink messages from SNS/SQS Queue into the S3 bucket
     * Azure - Sink messages delivered though Azure Service Bus into the Azure Container
     * */
    fun sinkMessageToStorage(cloudConfig: CloudConfig) {
        when (cloudConfig.provider) {
            "aws" -> {
                sinkSQSTopicSubscriptionToS3(cloudConfig)
            }
            "azure" -> {
                sinkAsbTopicSubscriptionToBlob(
                    cloudConfig.connectionString.toString(),
                    cloudConfig.storageAccountName.toString(),
                    cloudConfig.storageAccountKey.toString(),
                    cloudConfig.containerName.toString(),
                    cloudConfig.storageEndpoint.toString(),
                    cloudConfig.namespace.toString(),
                    cloudConfig.sharedAccessKeyName.toString(),
                    cloudConfig.sharedAccessKey.toString(),
                    cloudConfig.topicName.toString(),
                    cloudConfig.subscriptionName.toString(),
                )
            }
            else -> throw IllegalArgumentException("Unsupported cloud provider")
        }
    }

    /*
     * Sink messages from the given cloud provider messaging system into the respective storage
     * AWS - Sink messages from SNS/SQS Queue into the S3 bucket
     * */
    private fun sinkSQSTopicSubscriptionToS3(cloudConfig: CloudConfig) {
        // Initialize the Camel context
        val camelContext: CamelContext = DefaultCamelContext()

        // Configure AWS SQS Component
        val sqsComponent =
            AwsManager().configureAwsSQSComponent(
                cloudConfig.awsAccessKeyId.toString(),
                cloudConfig.awsSecretAccessKey.toString(),
                cloudConfig.awsSqsRegion.toString(),
            )

        // Override the service endpoint (if available). Ex. for LocalStack
        val sqsEndpoint = cloudConfig.awsSqsEndpoint
        if (sqsEndpoint != null && sqsEndpoint.isNotEmpty()) {
            sqsComponent.configuration.isOverrideEndpoint = true // Use public setter to enable override
            sqsComponent.configuration.uriEndpointOverride = sqsEndpoint // Pass the endpoint as a String
        }

        camelContext.addComponent("aws2-sqs", sqsComponent)

        // Configure AWS S3 Component
        val credentials = AwsBasicCredentials.create(cloudConfig.awsAccessKeyId, cloudConfig.awsSecretAccessKey)
        val region = Region.of(cloudConfig.awsS3Region)
        val s3Endpoint = cloudConfig.awsS3Endpoint

        val s3ClientBuilder =
            S3Client
                .builder()
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(region)

        // Override the service endpoint (if available). Ex. for LocalStack
        if (s3Endpoint != null && s3Endpoint.isNotEmpty()) {
            s3ClientBuilder.endpointOverride(URI.create(s3Endpoint))
        }

        val s3Client = s3ClientBuilder.build()

        // Configure S3 Component
        val s3Configuration =
            AWS2S3Configuration().apply {
                this.amazonS3Client = s3Client
                this.region = cloudConfig.awsS3Region
                this.bucketName = cloudConfig.awsS3BucketName
            }

        val s3Component =
            AWS2S3Component().apply {
                this.configuration = s3Configuration
            }
        camelContext.addComponent("aws2-s3", s3Component)

        // Add routes
        //     TODO: Remove delay for testing! Add: "?delay=2000"
        camelContext.addRoutes(
            object : RouteBuilder() {
                override fun configure() {
                    from("aws2-sqs://${cloudConfig.awsSqsQueueName}?delay=2000")
                        .process { exchange ->
                            val uniqueKey = "message-${System.currentTimeMillis()}.json"
                            exchange.message.setHeader("CamelAwsS3Key", uniqueKey)
                        }.to(
                            "aws2-s3://${cloudConfig.awsS3BucketName}?accessKey=${cloudConfig.awsAccessKeyId}&secretKey=${cloudConfig.awsSecretAccessKey}&region=${cloudConfig.awsS3Region}",
                        )
                }
            },
        )

        // Start the Camel context
        camelContext.start()
        Runtime.getRuntime().addShutdownHook(Thread { camelContext.stop() })
    }

    /*
     * Sink messages from the given cloud provider messaging system into the respective storage
     * Azure - Sink messages delivered though Azure Service Bus into the Azure Container
     * */
    private fun sinkAsbTopicSubscriptionToBlob(
        connectionString: String,
        accountName: String,
        accountKey: String,
        containerName: String,
        storageEndpoint: String?,
        serviceBusNamespace: String,
        sharedAccessKeyName: String,
        sharedAccessKey: String,
        topicName: String,
        subscriptionName: String,
    ) {
        // Initialize the Camel context
        val camelContext: CamelContext = DefaultCamelContext()

        // Configure the JMS component with AMQP over WebSocket
        configureAMQPComponent(connectionString, camelContext, serviceBusNamespace, sharedAccessKeyName, sharedAccessKey)

        // Construct the endpoint URI conditionally
        val fromUri =
            if (subscriptionName.isNullOrEmpty()) {
                "amqp:queue:$topicName"
            } else {
                "amqp:queue:$topicName/subscriptions/$subscriptionName"
            }

        // Add routes
        camelContext.addRoutes(
            object : RouteBuilder() {
                override fun configure() {
                    from(fromUri)
                        .process { exchange ->
                            val message = exchange.getIn().getBody(String::class.java)
                            exchange.message.setBody(message.toByteArray(Charsets.UTF_8))

                            val blobClientBuilder =
                                BlobServiceClientBuilder()
                                    .credential(StorageSharedKeyCredential(accountName, accountKey))

                            // Use the provided endpoint if available, else use the default endpoint
                            if (storageEndpoint.isNullOrEmpty()) {
                                blobClientBuilder.endpoint("https://$accountName.blob.core.windows.net")
                            } else {
                                blobClientBuilder.endpoint(storageEndpoint)
                            }

                            val blobClient =
                                blobClientBuilder
                                    .buildClient()
                                    .getBlobContainerClient(containerName)
                                    .getBlobClient("message-${System.currentTimeMillis()}.json")

                            try {
                                blobClient.upload(message.byteInputStream(), message.length.toLong(), true)
                                log.info("Successfully uploaded message to Blob Storage")
                            } catch (e: BlobStorageException) {
                                log.error("Error uploading to Blob Storage", e)
                            }
                        }
                }
            },
        )
        // Start the Camel context
        camelContext.start()
        Runtime.getRuntime().addShutdownHook(Thread { camelContext.stop() })
    }

    private fun configureAMQPComponent(
        connectionString: String,
        camelContext: CamelContext,
        serviceBusHostname: String,
        sharedAccessKeyName: String,
        sharedAccessKey: String,
    ) {
        // Create the AMQP URI using the connectionString and serviceBusHostname
        val endpoint =
            if (connectionString.isNotEmpty()) {
                connectionString
            } else {
                "amqps://$serviceBusHostname"
            }

        val connectionFactory = JmsConnectionFactory()
        connectionFactory.remoteURI = endpoint
        connectionFactory.username = sharedAccessKeyName
        connectionFactory.password = sharedAccessKey

        // Create the AMQP component
        val amqpComponent = AMQPComponent()
        amqpComponent.setCamelContext(camelContext)
        amqpComponent.connectionFactory = connectionFactory

        // Register the component
        camelContext.addComponent("amqp", amqpComponent)
    }
}
