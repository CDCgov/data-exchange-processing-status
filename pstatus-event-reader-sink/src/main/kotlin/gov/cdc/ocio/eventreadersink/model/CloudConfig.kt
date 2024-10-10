package gov.cdc.ocio.eventreadersink.model

data class CloudConfig(
    var provider: String, // "aws" or "azure"
    var awsConfig: AwsConfig? = null,
    var azureConfig: AzureConfig? = null
)
