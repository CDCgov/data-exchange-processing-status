package gov.cdc.ocio.reportschemavalidator.loaders

import gov.cdc.ocio.reportschemavalidator.models.ReportSchemaMetadata
import gov.cdc.ocio.reportschemavalidator.schema.SchemaStorageClient
import gov.cdc.ocio.reportschemavalidator.models.SchemaFile
import gov.cdc.ocio.reportschemavalidator.schema.BlobStorageSchemaClient
import gov.cdc.ocio.reportschemavalidator.schema.S3SchemaStorageClient


/**
 * A loader that retrieves schema files from cloud storage solutions like S3 or Azure Blob Storage.
 */
class CloudSchemaLoader(
    private val storageType: String,
    private val config: Map<String, String>
) : SchemaLoader {

    private val storageClient = createStorageClient()

    /**
     * The function which loads the schema based on the file name and returns a [SchemaFile]
     * @param fileName String
     * @return [SchemaFile]
     */
    override fun loadSchemaFile(fileName: String): SchemaFile {
        val content = storageClient.getSchemaFile(fileName)
        return SchemaFile(
            fileName = fileName,
            content = content
        )
    }

    /**
     * Provides a list of the schema files that are available.
     *
     * @return List<[ReportSchemaMetadata]>
     */
    override fun getSchemaFiles() = storageClient.getSchemaFiles()

    /**
     * Provides the schema loader information.
     *
     * @return SchemaLoaderInfo
     */
    override fun getInfo() = storageClient.getInfo()

    /**
     * Get the report schema content from the provided information.
     *
     * @param schemaFilename [String]
     * @return [Map]<[String], [Any]>
     */
    override fun getSchemaContent(schemaFilename: String) = storageClient.getSchemaContent(schemaFilename)

    /**
     * Get the report schema content from the provided information.
     *
     * @param schemaName [String]
     * @param schemaVersion [String]
     * @return [Map]<[String], [Any]>
     */
    override fun getSchemaContent(schemaName: String, schemaVersion: String) = storageClient.getSchemaContent(schemaName, schemaVersion)

    /**
     * Upserts a report schema -- if it does not exist it is added, otherwise the schema is replaced.  The schema is
     * validated before it is allowed to be upserted.
     *
     * @param schemaName [String]
     * @param schemaVersion [String]
     * @param content [String]
     * @return [String] - filename of the upserted report schema
     */
    override fun upsertSchema(schemaName: String, schemaVersion: String, content: String) = storageClient.upsertSchema(schemaName, schemaVersion, content)

    /**
     * Removes the schema file associated with the name and version provided.
     *
     * @param schemaName [String]
     * @param schemaVersion [String]
     * @return [String] - filename of the removed report schema
     */
    override fun removeSchema(schemaName: String, schemaVersion: String) = storageClient.removeSchema(schemaName, schemaVersion)

    override var healthCheckSystem = storageClient.healthCheckSystem

    /**
     * Factory function to create a storage client based on the provided configuration.
     * @return [SchemaStorageClient]
     */
    private fun createStorageClient(): SchemaStorageClient {
        return when (storageType) {
            "s3" -> {
                val bucketName = config["REPORT_SCHEMA_S3_BUCKET"] ?: throw IllegalArgumentException("S3 bucket not configured")
                val region = config["REPORT_SCHEMA_S3_REGION"] ?: "us-east-1"
                val roleArn = config["AWS_ROLE_ARN"] ?: ""
                val webIdentityTokenFile = config["AWS_WEB_IDENTITY_TOKEN_FILE"]
                S3SchemaStorageClient(system, bucketName, region, roleArn, webIdentityTokenFile)
            }
            "blob_storage" -> {
                val connectionString = config["REPORT_SCHEMA_BLOB_CONNECTION_STR"] ?: throw IllegalArgumentException("Blob connection string not configured")
                val containerName = config["REPORT_SCHEMA_BLOB_CONTAINER"] ?: "default-container"
                BlobStorageSchemaClient(system, connectionString, containerName)
            }
            else -> throw IllegalArgumentException("Unsupported storage type: $storageType")
        }
    }
}
