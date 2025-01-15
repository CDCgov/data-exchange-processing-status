package gov.cdc.ocio.reportschemavalidator.utils

import gov.cdc.ocio.reportschemavalidator.loaders.CachedSchemaLoader
import gov.cdc.ocio.reportschemavalidator.loaders.CloudSchemaLoader
import gov.cdc.ocio.reportschemavalidator.loaders.FileSchemaLoader
import gov.cdc.ocio.reportschemavalidator.loaders.SchemaLoader
import io.ktor.server.application.*
import io.ktor.server.config.*


/**
 * The class which is used to create the schema loader instance based on env vars
 */
object SchemaLoaderConfiguration {

    /**
     * The function which instantiates the CloudSchemaLoader based on the schema loader system type
     * @return CloudSchemaLoader
     */
    fun createSchemaLoader(environment: ApplicationEnvironment): SchemaLoader {
        val schemaLoaderSystem = environment.config.tryGetString("ktor.report_schema_loader_system")?: ""

        val schemaLoader = when (schemaLoaderSystem.lowercase()) {
            SchemaLoaderSystemType.S3.toString().lowercase() -> {
                val s3Bucket = environment.config.tryGetString("aws.s3.report_schema_bucket") ?: ""
                val s3Region = environment.config.tryGetString("aws.s3.report_schema_region") ?: ""
                val roleArn = environment.config.tryGetString("aws.role_arn") ?: ""
                val webIdentityTokenFile = environment.config.tryGetString("aws.web_identity_token_file") ?: ""
                val config = mapOf(
                    "REPORT_SCHEMA_S3_BUCKET" to s3Bucket,
                    "REPORT_SCHEMA_S3_REGION" to s3Region,
                    "AWS_ROLE_ARN" to roleArn,
                    "AWS_WEB_IDENTITY_TOKEN_FILE" to webIdentityTokenFile
                )
                CloudSchemaLoader(schemaLoaderSystem, config)
            }

            SchemaLoaderSystemType.BLOB_STORAGE.toString().lowercase() -> {
                val connectionString = environment.config.tryGetString("azure.blob_storage.report_schema_connection_string") ?: ""
                val container = environment.config.tryGetString("azure.blob_storage.report_schema_container") ?: ""
                val config = mapOf(
                    "REPORT_SCHEMA_BLOB_CONNECTION_STR" to connectionString,
                    "REPORT_SCHEMA_BLOB_CONTAINER" to container
                )
                CloudSchemaLoader(schemaLoaderSystem, config)
            }

            SchemaLoaderSystemType.FILE_SYSTEM.toString().lowercase() -> {
                val localFileSystemPath = environment.config.tryGetString("file_system.report_schema_local_path") ?: ""
                val config = mapOf(
                    "REPORT_SCHEMA_LOCAL_FILE_SYSTEM_PATH" to localFileSystemPath
                )
                FileSchemaLoader(config)
            }
            else -> throw IllegalArgumentException( "Unsupported schema loader type: $schemaLoaderSystem")
        }
        return CachedSchemaLoader(schemaLoader)
    }

}