package gov.cdc.ocio.eventreadersink.camel

import org.apache.camel.builder.RouteBuilder
import gov.cdc.ocio.eventreadersink.model.AwsConfig
import mu.KotlinLogging
import org.apache.camel.LoggingLevel

private val logger = KotlinLogging.logger {}

/**
 * AwsRoutes class is responsible for defining Camel routes that handle
 * the integration between AWS SQS and S3. It configures error handling
 * and processing of messages from an SQS queue to store them in an S3 bucket.
 *
 * @property awsConfig Configuration parameters for AWS services such as
 * SQS queue name, S3 bucket name, access key, and region.
 */

class AwsRoutes(private val awsConfig: AwsConfig) : RouteBuilder() {

    /**
     * Configures the Camel routes for processing messages from AWS SQS to S3.
     *
     * This method sets up error handling for exceptions during message processing,
     * defining retry logic and logging behavior. It also establishes the route
     * for reading messages from an SQS queue and writing them to an S3 bucket
     * with a dynamic filename based on the current timestamp.
     */
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
            .process { exchange ->
                exchange.message.setHeader("CamelAwsS3Key", "message-${System.currentTimeMillis()}.json")
            }.to("aws2-s3://${awsConfig.s3BucketName}?accessKey=${awsConfig.accessKeyId}&secretKey=${awsConfig.secretAccessKey}&region=${awsConfig.s3Region}")
            .log("Message sent to S3 bucket: ${awsConfig.s3BucketName}")
    }
}
