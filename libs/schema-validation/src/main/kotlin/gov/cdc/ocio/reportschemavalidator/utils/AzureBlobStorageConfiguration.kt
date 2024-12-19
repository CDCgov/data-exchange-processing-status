package gov.cdc.ocio.reportschemavalidator.utils

import io.ktor.server.config.*

/**
 * Blob storage configuration class
 * @param config ApplicationConfig
 * @param configurationPath String?
 */
class AzureBlobStorageConfiguration(config: ApplicationConfig, configurationPath: String? = null) {
    private val configPath = if (configurationPath != null) "$configurationPath." else ""
    val connectionString = config.tryGetString("${configPath}blob_storage.connection_string") ?: ""
    val container = config.tryGetString("${configPath}blob_storage.container") ?: ""

}