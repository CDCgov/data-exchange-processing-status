package gov.cdc.ocio.eventreadersink.cloud

import gov.cdc.ocio.eventreadersink.exceptions.ConfigurationException
import mu.KotlinLogging
import org.apache.camel.component.aws2.s3.AWS2S3Component
import org.apache.camel.component.aws2.s3.AWS2S3Configuration
import org.apache.camel.component.aws2.sqs.Sqs2Component
import org.apache.camel.component.aws2.sqs.Sqs2Configuration

private val logger = KotlinLogging.logger {}

/**
 * Class for creating Aws Components
 */
class AwsManager {
    /**
     * Configures and returns an instance of Sqs2Component for interacting with AWS SQS.
     *
     * This function initializes the SQS component with the provided AWS credentials
     * and region, allowing for communication with the specified SQS service.
     *
     * @param awsAccessKeyId The AWS access key ID for authentication.
     * @param awsSecretAccessKey The AWS secret access key for authentication.
     * @param awsRegion The AWS region where the SQS service is located.
     * @return An instance of Sqs2Component configured with the specified AWS credentials and region.
     * @throws IllegalArgumentException
     * @throws ConfigurationException
     */
    @Throws(IllegalArgumentException::class, ConfigurationException::class, Exception::class)
    fun configureAwsSQSComponent(
        awsAccessKeyId: String,
        awsSecretAccessKey: String,
        awsRegion: String,
        awsSqsQueueURL: String,
    ): Sqs2Component {
        try {
            val sqsComponent = Sqs2Component()
            val sqsConfiguration = Sqs2Configuration()
            sqsConfiguration.accessKey = awsAccessKeyId
            sqsConfiguration.secretKey = awsSecretAccessKey
            sqsConfiguration.region = awsRegion
            sqsComponent.configuration = sqsConfiguration

            // Override the service endpoint (if available). Ex. for LocalStack
            if (!awsSqsQueueURL.isNullOrEmpty()) {
                sqsComponent.configuration.isOverrideEndpoint = true // Use public setter to enable override
                sqsComponent.configuration.uriEndpointOverride = awsSqsQueueURL
            }

            return sqsComponent
        } catch (e: IllegalArgumentException) {
            logger.error("Invalid argument provided for SQS configuration: ${e.message}")
            throw e
        } catch (e: ConfigurationException) {
            logger.error("Error configuring AWS SQS component: ${e.message}")
            throw ConfigurationException("Failed to configure AWS SQS component: ${e.message}")
        } catch (e: Exception) {
            logger.error("An unexpected error occurred configuring SQS component: ${e.message}")
            throw e
        }
    }

    /**
     * Configures and returns an instance of AWS2S3Component for interacting with AWS S3.
     *
     * This function initializes the S3 component with the provided AWS credentials
     * and region, allowing for communication with the specified S3 service.
     *
     * @param awsAccessKeyId The AWS access key ID for authentication.
     * @param awsSecretAccessKey The AWS secret access key for authentication.
     * @param awsRegion The AWS region where the S3 service is located.
     * @return An instance of AWS2S3Component configured with the specified AWS credentials and region.
     * @throws IllegalArgumentException
     * @throws ConfigurationException
     */
    @Throws(IllegalArgumentException::class, ConfigurationException::class)
    fun configureAwsS3Component(
        awsAccessKeyId: String,
        awsSecretAccessKey: String,
        awsRegion: String,
        awsS3EndpointURL: String?,
    ): AWS2S3Component {
        try {
            val s3Component = AWS2S3Component()
            val s3Configuration = AWS2S3Configuration()
            s3Configuration.accessKey = awsAccessKeyId
            s3Configuration.secretKey = awsSecretAccessKey
            s3Configuration.region = awsRegion
            s3Component.configuration = s3Configuration

            // Override the service endpoint (if available). Ex. for LocalStack
            if (!awsS3EndpointURL.isNullOrEmpty()) {
                s3Component.configuration.isOverrideEndpoint = true // Use public setter to enable override
                s3Component.configuration.uriEndpointOverride = awsS3EndpointURL
            }

            return s3Component
        } catch (e: IllegalArgumentException) {
            logger.error("Invalid argument provided for S3 configuration: ${e.message}")
            throw e
        } catch (e: Exception) {
            logger.error("Error configuring AWS S3 component: ${e.message}")
            throw ConfigurationException("Failed to configure AWS S3 component: ${e.message}")
        }
    }
}
