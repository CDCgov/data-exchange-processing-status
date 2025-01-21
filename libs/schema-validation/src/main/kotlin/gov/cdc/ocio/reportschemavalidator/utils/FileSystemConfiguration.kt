package gov.cdc.ocio.reportschemavalidator.utils

import io.ktor.server.config.*

/**
 * File system configuration class
 *
 * @param config ApplicationConfig
 * @param configurationPath String?
 */
class FileSystemConfiguration(config: ApplicationConfig, configurationPath: String? = null) {
    private val configPath = configurationPath ?: ""
    val localFileSystemPath = config.tryGetString("${configPath}.report_schema_local_path") ?: ""
}