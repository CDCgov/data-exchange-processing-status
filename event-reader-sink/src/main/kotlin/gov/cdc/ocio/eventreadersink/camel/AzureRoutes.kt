package gov.cdc.ocio.eventreadersink.camel

import com.azure.storage.blob.BlobServiceClient
import com.azure.storage.blob.BlobServiceClientBuilder
import com.azure.storage.blob.models.BlobStorageException
import com.azure.storage.common.StorageSharedKeyCredential
import mu.KotlinLogging
import org.apache.camel.builder.RouteBuilder

private val logger = KotlinLogging.logger {}

/**
 * AzureRoutes is a Camel RouteBuilder that defines the routes for processing
 * messages from an Azure Service Bus topic subscription and uploading them
 * to Azure Blob Storage.
 *
 * @property topicName The name of the Azure Service Bus topic.
 * @property subscriptionName The name of the subscription to the topic.
 * @property accountName The Azure storage account name.
 * @property accountKey The Azure storage account key.
 * @property containerName The name of the Blob Storage container.
 * @property timeProvider Inject a time provider for controllable time in tests.
 * @property customFromUri Optional custom URI for testing
 */
class AzureRoutes(
    private val topicName: String,
    private val subscriptionName: String,
    private val accountName: String,
    private val accountKey: String,
    private val containerName: String,
    private val storageEndpointURL: String?,
    private val timeProvider: () -> Long = { System.currentTimeMillis() },
    private val customFromUri: String? = null,
) : RouteBuilder() {
    /**
     * Configures the Camel routes.
     *
     * This method defines a route that listens to messages from the specified
     * Azure Service Bus topic subscription and processes them by uploading
     * to Azure Blob Storage.
     */
    override fun configure() {
        // Define error handling for exceptions that may occur during message processing
        onException(BlobStorageException::class.java)
            .maximumRedeliveries(3) // Set the number of retry attempts
            .redeliveryDelay(1000) // Set the delay between retries (in milliseconds)
            .backOffMultiplier(2.0) // Exponential backoff
            .handled(true) // Mark as handled
            .process { exchange ->
                val exception = exchange.getProperty(org.apache.camel.Exchange.EXCEPTION_CAUGHT, BlobStorageException::class.java)
                logger.error("BlobStorageException occurred: ${exception.message}", exception)
            }

        onException(Exception::class.java)
            .handled(true)
            .process { exchange ->
                val exception = exchange.getProperty(org.apache.camel.Exchange.EXCEPTION_CAUGHT, Exception::class.java)
                logger.error("Unexpected error occurred: ${exception.message}", exception)
            }

        // Use custom URI if provided, otherwise construct from topic and subscription names
        val fromUri =
            customFromUri
                ?: if (subscriptionName.isNullOrEmpty()) {
                    "amqp:queue:$topicName"
                } else {
                    "amqp:queue:$topicName/subscriptions/$subscriptionName"
                }

        // Set up the BlobServiceClient with either Azurite or Azure endpoints
        val blobServiceClient = createBlobServiceClient()
        context.registry.bind("myBlobServiceClient", blobServiceClient)

        val azureBlobEndpoint =
            "azure-storage-blob://$accountName/$containerName" +
                "?operation=uploadBlockBlob" +
                "&blobServiceClient=#myBlobServiceClient" // Use the registered BlobServiceClient

        // Define the Camel Route for Azure Components: ServiceBus Topic -> BlobStorage
        from(fromUri)
            .routeId("azureBlobRoute")
            .process { exchange ->
                val message = exchange.getIn().getBody(String::class.java)
                logger.info("Received message from Azure Service Bus: $message")
                exchange.message.setBody(message.toByteArray(Charsets.UTF_8))

                // Set a unique blob name based on the timestamp
                val timestamp = timeProvider()
                exchange.message.setHeader("CamelAzureStorageBlobBlobName", "message-$timestamp.json")
            }.to(azureBlobEndpoint) // Send the message to Azure Blob Storage
            .log("Message sent to Azure Blob container: $containerName with blob name: message-${timeProvider()}.json")
    }

    /**
     * Creates and returns a BlobContainerClient based on whether a custom endpoint URL is provided.
     */
    private fun createBlobServiceClient(): BlobServiceClient {
        val blobClientBuilder =
            BlobServiceClientBuilder()
                .credential(StorageSharedKeyCredential(accountName, accountKey))

        // Use custom endpoint for Azurite, or default to Azure Blob Storage endpoint
        if (!storageEndpointURL.isNullOrEmpty()) {
            val customEndpoint =
                if (storageEndpointURL.endsWith(accountName)) {
                    storageEndpointURL
                } else {
                    "$storageEndpointURL/$accountName"
                }
            blobClientBuilder.endpoint(customEndpoint)
        } else {
            blobClientBuilder.endpoint("https://$accountName.blob.core.windows.net")
        }

        return blobClientBuilder.buildClient()
    }
}
