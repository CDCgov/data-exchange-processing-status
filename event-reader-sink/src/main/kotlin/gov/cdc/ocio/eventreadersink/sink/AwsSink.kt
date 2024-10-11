package gov.cdc.ocio.eventreadersink.sink

import gov.cdc.ocio.eventreadersink.camel.AwsRoutes
import org.apache.camel.CamelContext
import org.apache.camel.impl.DefaultCamelContext
import gov.cdc.ocio.eventreadersink.cloud.AwsManager
import gov.cdc.ocio.eventreadersink.exceptions.ConfigurationException
import gov.cdc.ocio.eventreadersink.model.AwsConfig
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}


/**
 * AwsSink is responsible for managing the sinking of messages from AWS SQS/SNS to S3
 * using Apache Camel routes. It handles the configuration of AWS components
 * required for processing messages and ensures that the Camel context is properly
 * initialized and started for message flow.
 *
 * This class encapsulates the logic needed to bridge AWS SQS and S3, allowing for
 * seamless transfer of messages to storage in S3 buckets.
 */
class AwsSink {

    /**
     * Sinks messages from an AWS SQS topic subscription to an S3 bucket.
     *
     * This function initializes the Camel context, configures AWS components,
     * and starts the Camel route for processing messages. It also sets up a shutdown
     * hook to stop the Camel context gracefully when the application is terminated.
     *
     * @param awsConfig The AWS configuration required for connecting to SQS and S3.
     * @throws RuntimeException if an error occurs during the setup of AWS components
     *         or starting the Camel context.
     */
    @Throws(ConfigurationException:: class, Exception:: class)
    fun sinkSQSTopicSubscriptionToS3(awsConfig: AwsConfig) {

        try {
            // Initialize the Camel context
            val camelContext: CamelContext = DefaultCamelContext()

            // Configure AWS Components
            configureAwsComponents(awsConfig, camelContext)

            // Add routes
            camelContext.addRoutes(AwsRoutes(awsConfig))

            // Start the Camel context
            camelContext.start()
            Runtime.getRuntime().addShutdownHook(Thread { camelContext.stop() })
        } catch (e: ConfigurationException) {
            logger.error("Error configuring AWS components: ${e.message}", e)
            throw ConfigurationException("Error configuring AWS components:: ${e.message}")
        } catch (e: Exception) {
            logger.error("Error while sinking messages to S3: ${e.message}", e)
            throw e
        }
    }

    /**
     * Configures AWS components for SQS and S3 in the provided Camel context.
     *
     * This function initializes the SQS and S3 components with the specified AWS
     * credentials and region settings. It adds these components to the given
     * Camel context, enabling the routes to interact with AWS services.
     *
     * @param awsConfig The AWS configuration containing access keys and region details.
     * @param camelContext The Camel context where the components will be registered.
     * @throws ConfigurationException if an error occurs while configuring the AWS components.
     * @throws Exception
     */
    @Throws(ConfigurationException::class, Exception::class)
    private fun configureAwsComponents(awsConfig: AwsConfig, camelContext: CamelContext) {

        try {
            val sqsComponent = AwsManager().configureAwsSQSComponent(
                awsConfig.accessKeyId,
                awsConfig.secretAccessKey,
                awsConfig.sqsRegion
            )
            camelContext.addComponent("aws2-sqs", sqsComponent)

            val s3Component = AwsManager().configureAwsS3Component(
                awsConfig.accessKeyId,
                awsConfig.secretAccessKey,
                awsConfig.s3Region
            )
            camelContext.addComponent("aws2-s3", s3Component)

        } catch (e: Exception) {
            logger.error("Error configuring AWS components: ${e.message}", e)
            throw ConfigurationException("Failed to configure AWS components: ${e.message}")
        }
    }
}
