package gov.cdc.ocio.eventreadersink.model

data class AzureConfig(
    var namespace: String,
    var connectionString: String,
    var sharedAccessKeyName: String,
    var sharedAccessKey: String,
    var topicName: String,
    var subscriptionName: String,
    var containerName: String,
    var storageAccountKey: String,
    var storageAccountName: String
)