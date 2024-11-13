package gov.cdc.ocio.eventreadersink.model

/**
 * Represents the configuration settings required for interacting with AWS services.
 *
 * This data class holds the necessary credentials and settings to access AWS SQS and S3 services.
 *
 * @property accessKeyId The AWS access key ID used for authentication with AWS services.
 * @property secretAccessKey The AWS secret access key used for authentication with AWS services.
 * @property sqsQueueName The name of the AWS SQS queue.
 * @property sqsQueueURL The URL of the AWS SQS queue.
 * @property sqsRegion The AWS region where the SQS service is located.
 * @property s3BucketName The name of the AWS S3 bucket.
 * @property s3Region The AWS region where the S3 service is located.
 */
data class AwsConfig(
    var accessKeyId: String,
    var secretAccessKey: String,
    var sqsQueueName: String,
    var sqsQueueURL: String,
    var sqsRegion: String,
    var s3EndpointURL: String?,
    var s3BucketName: String,
    var s3Region: String
)