package gov.cdc.ocio.eventreadersink.model

/**
 * Enum class representing the different cloud provider types.
 *
 * This enum defines the supported cloud providers for the application,
 * allowing for easy reference and type safety when dealing with cloud
 * service configurations and integrations.
 *
 * @property value The string representation of the cloud provider type.
 */
enum class CloudProviderType (val value: String) {
    AWS("aws"),
    AZURE ("azure")
}