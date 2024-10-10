package gov.cdc.ocio.eventreadersink.camel

import org.apache.camel.builder.RouteBuilder
import gov.cdc.ocio.eventreadersink.model.AwsConfig

class AwsRoutes(private val awsConfig: AwsConfig) : RouteBuilder() {
    override fun configure() {
        from("aws2-sqs://${awsConfig.sqsQueueName}")
            .setHeader("CamelAwsS3Key", simple("message-${System.currentTimeMillis()}.json"))
            .to("aws2-s3://${awsConfig.s3BucketName}?accessKey=${awsConfig.accessKeyId}&secretKey=${awsConfig.secretAccessKey}&region=${awsConfig.s3Region}")
    }
}
