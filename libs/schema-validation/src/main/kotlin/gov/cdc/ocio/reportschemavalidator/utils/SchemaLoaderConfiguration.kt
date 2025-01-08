package gov.cdc.ocio.reportschemavalidator.utils


import gov.cdc.ocio.reportschemavalidator.loaders.CloudSchemaLoader
import gov.cdc.ocio.reportschemavalidator.loaders.FileSchemaLoader
import gov.cdc.ocio.reportschemavalidator.loaders.SchemaLoader
import io.ktor.server.application.*
import io.ktor.server.config.*

/**
 * The class which is used to create the schema loader instance based on env vars
 * @param environment ApplicationEnvironment
 */
class SchemaLoaderConfiguration(environment: ApplicationEnvironment){
    private val schemaLoaderSystem = environment.config.tryGetString("ktor.report_schema_loader_system")?: ""
    private val s3Bucket = environment.config.tryGetString("aws.s3.report_schema_bucket") ?: ""
    private val s3Region = environment.config.tryGetString("aws.s3.report_schema_region") ?: ""
    private val roleArn = environment.config.tryGetString("aws.role_arn") ?: ""
    private val webIdentityTokenFile = environment.config.tryGetString("aws.web_identity_token_file") ?: ""
    private val connectionString = environment.config.tryGetString("azure.blob_storage.report_schema_connection_string") ?: ""
    private val container = environment.config.tryGetString("azure.blob_storage.report_schema_container") ?: ""
    private val localFileSystemPath = environment.config.tryGetString("file_system.report_schema_local_path") ?: ""
    /**
     * The function which instantiates the CloudSchemaLoader based on the schema loader system type
     * @return CloudSchemaLoader
     */
    fun createSchemaLoader(): SchemaLoader {
        when (schemaLoaderSystem.lowercase()) {
            SchemaLoaderSystemType.S3.toString().lowercase()  -> {
                val config = mapOf(
                    "REPORT_SCHEMA_S3_BUCKET" to s3Bucket,
                    "REPORT_SCHEMA_S3_REGION" to s3Region,
                    "AWS_ROLE_ARN" to roleArn,
                    "AWS_WEB_IDENTITY_TOKEN_FILE" to webIdentityTokenFile
                )
                return CloudSchemaLoader(schemaLoaderSystem, config)
            }

            SchemaLoaderSystemType.BLOB_STORAGE.toString().lowercase() -> {
                val config = mapOf(
                    "REPORT_SCHEMA_BLOB_CONNECTION_STR" to connectionString,
                    "REPORT_SCHEMA_BLOB_CONTAINER" to container
                )
                return CloudSchemaLoader(schemaLoaderSystem, config)
            }

            SchemaLoaderSystemType.FILE_SYSTEM.toString().lowercase() -> {
                val config = mapOf(
                    "REPORT_SCHEMA_LOCAL_FILE_SYSTEM_PATH" to localFileSystemPath

                )
                return FileSchemaLoader(config)
            }
            else ->throw IllegalArgumentException( "Unsupported schema loader type: $schemaLoaderSystem")

        }

    }

}