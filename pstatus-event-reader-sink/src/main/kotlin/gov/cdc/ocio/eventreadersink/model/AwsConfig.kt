package gov.cdc.ocio.eventreadersink.model

data class AwsConfig(
    var accessKeyId: String,
    var secretAccessKey: String,
    var sqsQueueName: String,
    var sqsQueueURL: String,
    var sqsRegion: String,
    var s3BucketName: String,
    var s3Region: String
)