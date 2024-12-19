package gov.cdc.ocio.reportschemavalidator.utils


import gov.cdc.ocio.reportschemavalidator.loaders.CloudSchemaLoader
import io.ktor.server.application.*
import io.ktor.server.config.*

/**
 * The class which is used to create the schema loader instance based on env vars
 * @param environment ApplicationEnvironment
 */
class CloudSchemaLoaderConfiguration(environment: ApplicationEnvironment){
    private val schemaLoaderSystem = environment.config.tryGetString("ktor.schema_loader_system")?: ""
    private val s3Bucket = environment.config.tryGetString("aws.s3.report_schema_bucket") ?: ""
    private val s3Region = environment.config.tryGetString("aws.s3.report_schema_region") ?: ""
    private val connectionString = environment.config.tryGetString("azure.blob_storage.connection_string") ?: ""
    private val container = environment.config.tryGetString("azure.blob_storage.container") ?: ""

    /**
     * The function which instantiates the CloudSchemaLoader based on the schema loader system type
     * @return CloudSchemaLoader
     */
    fun createSchemaLoader(): CloudSchemaLoader {
        when (schemaLoaderSystem.lowercase()) {
            SchemaLoaderSystemType.S3.toString().lowercase()  -> {
                val config = mapOf(
                    "REPORT_SCHEMA_S3_BUCKET" to s3Bucket,
                    "REPORT_SCHEMA_S3_REGION" to s3Region
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
            else ->throw IllegalArgumentException( "Unsupported schema loader type: $schemaLoaderSystem")

        }

    }

}