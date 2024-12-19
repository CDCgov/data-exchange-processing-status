package gov.cdc.ocio.reportschemavalidator.utils

import io.ktor.server.config.*

/**
 * AWS S3 configuration class
 * @param config ApplicationConfig
 * @param configurationPath String?
 */
class AWSS3Configuration(config: ApplicationConfig, configurationPath: String? = null) {
    private val configPath = if (configurationPath != null) "$configurationPath." else ""
    val s3Bucket = config.tryGetString("${configPath}s3.report_schema_bucket") ?: ""
    val s3Region = config.tryGetString("${configPath}s3.report_schema_region") ?: ""

}