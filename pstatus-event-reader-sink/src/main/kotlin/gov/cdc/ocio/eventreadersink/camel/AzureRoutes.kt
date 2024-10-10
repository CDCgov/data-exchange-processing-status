package gov.cdc.ocio.eventreadersink.camel

import com.azure.storage.blob.BlobServiceClientBuilder
import com.azure.storage.blob.models.BlobStorageException
import org.apache.camel.builder.RouteBuilder
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

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
 */
class AzureRoutes(
    private val topicName: String,
    private val subscriptionName: String,
    private val accountName: String,
    private val accountKey: String,
    private val containerName: String
) : RouteBuilder() {

    /**
     * Configures the Camel routes.
     *
     * This method defines a route that listens to messages from the specified
     * Azure Service Bus topic subscription and processes them by uploading
     * to Azure Blob Storage.
     */
    override fun configure() {
        from("amqp:queue:$topicName/subscriptions/$subscriptionName")
            .process { exchange ->
                val message = exchange.getIn().getBody(String::class.java)
                log.info("Received message from Azure Service Bus: $message")
                exchange.message.setBody(message.toByteArray(Charsets.UTF_8))
                uploadToBlobStorage(message)
            }
    }

    /**
     * Uploads a message to Azure Blob Storage.
     *
     * @param message The message content to upload as a blob.
     */
    private fun uploadToBlobStorage(message: String) {
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
