package gov.cdc.ocio.eventreadersink.model

/**
 * Represents the cloud provider configuration for the application.
 *
 * This data class allows for the selection between AWS and Azure configurations,
 * encapsulating the settings necessary for accessing the respective cloud services.
 *
 * @property provider provider The cloud provider to use, represented by the CloudProviderType enum.
 * @property awsConfig Optional configuration for AWS services, represented by an AwsConfig instance.
 * @property azureConfig Optional configuration for Azure services, represented by an AzureConfig instance.
 */
data class CloudConfig(
    var provider: CloudProviderType,
    var awsConfig: AwsConfig? = null,
    var azureConfig: AzureConfig? = null
)
