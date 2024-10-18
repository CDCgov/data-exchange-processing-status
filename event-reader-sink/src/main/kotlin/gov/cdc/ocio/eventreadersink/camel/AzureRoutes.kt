package gov.cdc.ocio.eventreadersink.camel

import com.azure.storage.blob.BlobClient
import com.azure.storage.blob.BlobServiceClientBuilder
import com.azure.storage.blob.models.BlobStorageException
import com.azure.storage.common.StorageSharedKeyCredential
import gov.cdc.ocio.eventreadersink.exceptions.BadStateException
import org.apache.camel.builder.RouteBuilder
import mu.KotlinLogging

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
 */
class AzureRoutes(
    private val topicName: String,
    private val subscriptionName: String,
    private val accountName: String,
    private val accountKey: String,
    private val containerName: String,
    private val storageEndpointURL: String?
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

        // Construct the endpoint URI conditionally
        val fromUri =
            if (subscriptionName.isNullOrEmpty()) {
                "amqp:queue:$topicName"
            } else {
                "amqp:queue:$topicName/subscriptions/$subscriptionName"
            }

        //Define the Camel Route for Azure Components: ServiceBus Topic -> BlobStorage
        from(fromUri)
            .process { exchange ->
                val message = exchange.getIn().getBody(String::class.java)
                logger.info("Received message from Azure Service Bus: $message")
                exchange.message.setBody(message.toByteArray(Charsets.UTF_8))
                uploadToBlobStorage(message)
            }
    }

    /**
     * Uploads a message to Azure Blob Storage.
     * @param message The message content to upload as a blob.
     */
    @Throws(BlobStorageException:: class, Exception:: class)
    private fun uploadToBlobStorage(message: String) {
        try {
            val blobClientBuilder =
                BlobServiceClientBuilder()
                    .credential(StorageSharedKeyCredential(accountName, accountKey))

                    // Use the provided endpoint if available, else use the default endpoint
                    if (storageEndpointURL.isNullOrEmpty()) {
                        blobClientBuilder.endpoint("https://$accountName.blob.core.windows.net")
                    } else {
                        blobClientBuilder.endpoint(storageEndpointURL)
                    }

            val blobClient = blobClientBuilder
//                .connectionString("DefaultEndpointsProtocol=https;AccountName=$accountName;AccountKey=$accountKey;EndpointSuffix=core.windows.net")
                .buildClient()
                .getBlobContainerClient(containerName)
                .getBlobClient("message-${System.currentTimeMillis()}.json")

            blobClient.upload(message.byteInputStream(), message.length.toLong(), true)
        } catch (e: BlobStorageException) {
            logger.error("Error uploading to Blob Storage", e)
            throw e
        } catch (e: Exception) {
            // Handle any other exceptions that might occur
            logger.error("Unexpected error occurred while uploading to Blob Storage", e)
            throw e
        }
    }
}
