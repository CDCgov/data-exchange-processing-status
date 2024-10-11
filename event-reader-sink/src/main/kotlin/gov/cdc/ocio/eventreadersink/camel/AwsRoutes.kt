package gov.cdc.ocio.eventreadersink.camel

import org.apache.camel.builder.RouteBuilder
import gov.cdc.ocio.eventreadersink.model.AwsConfig
import mu.KotlinLogging
import org.apache.camel.LoggingLevel

private val logger = KotlinLogging.logger {}

class AwsRoutes(private val awsConfig: AwsConfig) : RouteBuilder() {
    override fun configure() {

        // Define error handling for exceptions that may occur during message processing
        onException(Exception::class.java)
            .maximumRedeliveries(3) // Set the number of retry attempts
            .redeliveryDelay(1000) // Delay between retries (1 second)
            .backOffMultiplier(2.0) // Exponential backoff
            .handled(true) // Prevent the exception from propagating
            .log(LoggingLevel.ERROR, "Error processing AWS route: \${exception.message}")
            .process { exchange ->
                val exception = exchange.getProperty(org.apache.camel.Exchange.EXCEPTION_CAUGHT, Exception::class.java)
                logger.error("Exception occurred: ${exception.message}", exception)
            }

        //Define the Camel Route for AWS Components: SQS/SNS Topic -> S3
        from("aws2-sqs://${awsConfig.sqsQueueName}")
            .setHeader("CamelAwsS3Key", simple("message-${System.currentTimeMillis()}.json"))
            .to("aws2-s3://${awsConfig.s3BucketName}?accessKey=${awsConfig.accessKeyId}&secretKey=${awsConfig.secretAccessKey}&region=${awsConfig.s3Region}")
            .log("Message sent to S3 bucket: ${awsConfig.s3BucketName}")
    }
}
