package gov.cdc.ocio.eventreadersink.model

/**
 * Represents the configuration settings required for interacting with Azure services.
 *
 * This data class holds the necessary credentials and settings to access Azure Service Bus and Blob Storage.
 *
 * @property namespace The namespace of the Azure Service Bus.
 * @property connectionString The connection string for accessing the Azure Storage account.
 * @property sharedAccessKeyName The name of the shared access key for the Azure Storage account.
 * @property sharedAccessKey The shared access key for the Azure Storage account.
 * @property topicName The name of the Azure Service Bus topic.
 * @property subscriptionName The name of the subscription for the Azure Service Bus topic.
 * @property containerName The name of the Blob Storage container.
 * @property storageAccountKey The storage account key for the Azure Storage account.
 * @property storageAccountName The storage account name for the Azure Storage account.
 * @property storageEndpointURL The custom URL for Azure Storage (for Azurite or left null for default Azure endpoint).
 */
data class AzureConfig(
    var namespace: String,
    var connectionString: String,
    var sharedAccessKeyName: String,
    var sharedAccessKey: String,
    var topicName: String,
    var subscriptionName: String,
    var containerName: String,
    var storageAccountKey: String,
    var storageAccountName: String,
    var storageEndpointURL: String?
)