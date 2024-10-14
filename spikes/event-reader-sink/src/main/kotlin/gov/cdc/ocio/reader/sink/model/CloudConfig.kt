package gov.cdc.ocio.reader.sink.model

data class CloudConfig(
    var provider: String, // "aws" or "azure"
    // AWS Config Values
    var awsAccessKeyId: String? = null,
    var awsSecretAccessKey: String? = null,
    var awsSqsEndpoint: String? = null,
    var awsSqsQueueName: String? = null,
    var awsSqsQueueURL: String? = null,
    var awsSqsRegion: String? = null,
    var awsS3Endpoint: String? = null,
    var awsS3BucketName: String? = null,
    var awsS3Region: String? = null,
    // Azure Config Values
    var namespace: String? = null,
    var connectionString: String? = null,
    var sharedAccessKeyName: String? = null,
    var sharedAccessKey: String? = null,
    var topicName: String? = null,
    var subscriptionName: String? = null,
    var containerName: String? = null,
    var storageAccountKey: String? = null,
    var storageAccountName: String? = null,
)
