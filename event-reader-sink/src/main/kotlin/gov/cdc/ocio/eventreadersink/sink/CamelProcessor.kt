package gov.cdc.ocio.eventreadersink.sink

import gov.cdc.ocio.eventreadersink.model.CloudProviderType
import gov.cdc.ocio.eventreadersink.exceptions.BadStateException
import gov.cdc.ocio.eventreadersink.model.AwsConfig
import gov.cdc.ocio.eventreadersink.model.AzureConfig
import gov.cdc.ocio.eventreadersink.model.CloudConfig
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * The CamelProcessor class is responsible for sinking messages to storage
 * based on the provided cloud configuration. It interacts with different
 * cloud providers, specifically AWS and Azure, to store messages in their
 * respective storage services.
 *
 * This class provides functionality to process cloud configurations and
 * perform the appropriate actions based on the specified cloud provider.
 */
class CamelProcessor {

    /**
     * Sinks messages to storage based on the provided cloud configuration.
     *
     * This method checks the cloud provider specified in the CloudConfig.
     * If the provider is AWS, it invokes the appropriate AWS sink method.
     * If the provider is Azure, it invokes the appropriate Azure sink method.
     *
     * @param cloudConfig An instance of CloudConfig containing configuration details
     *                    for the selected cloud provider.
     * @throws IllegalArgumentException if the cloud provider is unsupported or
     *                                   configuration is missing.
     */
    @Throws(BadStateException:: class, IllegalArgumentException:: class, Exception::class)
    fun sinkMessageToStorage(cloudConfig: CloudConfig) {
        try {
            val provider = cloudConfig.provider

            when (provider) {
                CloudProviderType.AWS -> {
                    val awsConfig: AwsConfig? = cloudConfig.awsConfig
                    if (awsConfig != null) {
                        logger.info("Sinking message to S3 for AWS configuration.")
                        AwsSink().sinkSQSTopicSubscriptionToS3(awsConfig)
                    } else {
                        logger.error("AWS configuration is missing.")
                        throw IllegalArgumentException("AWS configuration is missing.")
                    }
                }
                CloudProviderType.AZURE -> {
                    val azureConfig: AzureConfig? = cloudConfig.azureConfig
                    if (azureConfig != null) {
                        logger.info ("Sinking message to Blob Storage for Azure configuration.")
                        AzureSink().sinkAsbTopicSubscriptionToBlob(
                            azureConfig.connectionString,
                            azureConfig.storageEndpointURL,
                            azureConfig.storageAccountName,
                            azureConfig.storageAccountKey,
                            azureConfig.containerName,
                            azureConfig.namespace,
                            azureConfig.sharedAccessKeyName,
                            azureConfig.sharedAccessKey,
                            azureConfig.topicName,
                            azureConfig.subscriptionName
                        )
                    } else {
                        logger.error("Azure configuration is missing.")
                        throw IllegalArgumentException("Azure configuration is missing.")
                    }
                }
            }
        } catch (e: BadStateException) {
            logger.error ("Error processing cloud configuration: ${e.message}" )
            throw e
        } catch (e: Exception) {
            logger.error("Error sinking message to storage due to an unexpected error: ${e.message}")
            throw e
        }
    }
}
