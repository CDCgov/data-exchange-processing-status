package gov.cdc.ocio.eventreadersink.sink

import gov.cdc.ocio.eventreadersink.cloud.CloudProviderType
import gov.cdc.ocio.eventreadersink.model.AwsConfig
import gov.cdc.ocio.eventreadersink.model.AzureConfig
import gov.cdc.ocio.eventreadersink.model.CloudConfig

/**
 * CamelProcessor is responsible for handling the processing of messages
 * from different cloud providers and sinking them to the appropriate storage.
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
    @Throws(IllegalArgumentException::class)
    fun sinkMessageToStorage(cloudConfig: CloudConfig) {
        val provider = cloudConfig.provider

        when (provider) {
            CloudProviderType.AWS.value -> {
                val awsConfig: AwsConfig? = cloudConfig.awsConfig
                if (awsConfig != null) {
                    createAwsSink().sinkSQSTopicSubscriptionToS3(awsConfig)
                } else {
                    throw IllegalArgumentException("AWS configuration is missing.")
                }
            }
            CloudProviderType.AZURE.value -> {
                val azureConfig: AzureConfig? = cloudConfig.azureConfig
                if (azureConfig != null) {
                    createAzureSink().sinkAsbTopicSubscriptionToBlob(
                        azureConfig.connectionString,
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
                    throw IllegalArgumentException("Azure configuration is missing.")
                }
            }
            else -> throw IllegalArgumentException("Unsupported cloud provider")
        }
    }

    /**
     * Creates an instance of AwsSink.
     *
     * @return An instance of AwsSink.
     */
    private fun createAwsSink(): AwsSink {
        return AwsSink()
    }

    /**
     * Creates an instance of AzureSink.
     *
     * @return An instance of AzureSink.
     */
    private fun createAzureSink(): AzureSink {
        return AzureSink()
    }
}
