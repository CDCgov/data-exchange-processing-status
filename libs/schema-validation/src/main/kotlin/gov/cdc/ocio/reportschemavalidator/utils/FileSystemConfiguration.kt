package gov.cdc.ocio.reportschemavalidator.utils

import io.ktor.server.config.*

/**
 * Blob storage configuration class
 * @param config ApplicationConfig
 * @param configurationPath String?
 */
class FileSystemConfiguration(config: ApplicationConfig, configurationPath: String? = null) {
    private val configPath = if (configurationPath != null) "$configurationPath." else ""
    val localFileSystemPath = config.tryGetString("${configPath}.report_schema_local_path") ?: ""

}