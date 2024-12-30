package gov.cdc.ocio.reportschemavalidator.loaders

import gov.cdc.ocio.reportschemavalidator.schema.SchemaStorageClient
import gov.cdc.ocio.reportschemavalidator.models.SchemaFile
import gov.cdc.ocio.reportschemavalidator.schema.BlobStorageSchemaClient
import gov.cdc.ocio.reportschemavalidator.schema.S3SchemaStorageClient



/**
 * A loader that retrieves schema files from cloud storage solutions like S3 or Azure Blob Storage.
 */
class CloudSchemaLoader(private val storageType: String, private val config: Map<String, String>) : SchemaLoader {

    private val storageClient: SchemaStorageClient = createStorageClient()

    /**
     * The function which loads the schema based on the file name and returns a [SchemaFile]
     * @param fileName String
     * @return [SchemaFile]
     */
    override fun loadSchemaFile(fileName: String): SchemaFile {
        val inputStream = storageClient.getSchemaFile(fileName)
        return SchemaFile(
            fileName = fileName,
            inputStream = inputStream
        )
    }

    /**
     * Factory function to create a storage client based on the provided configuration.
     * @return [SchemaStorageClient]
     */
    private fun createStorageClient(): SchemaStorageClient {
        return when (storageType) {
            "s3" -> {
                val bucketName = config["REPORT_SCHEMA_S3_BUCKET"] ?: throw IllegalArgumentException("S3 bucket not configured")
                val region = config["REPORT_SCHEMA_S3_REGION"] ?: "us-east-1"
                S3SchemaStorageClient(bucketName, region)
            }
            "blob_storage" -> {
                val connectionString = config["REPORT_SCHEMA_BLOB_CONNECTION_STR"] ?: throw IllegalArgumentException("Blob connection string not configured")
                val containerName = config["REPORT_SCHEMA_BLOB_CONTAINER"] ?: "default-container"
                BlobStorageSchemaClient(connectionString, containerName)
            }

            else -> throw IllegalArgumentException("Unsupported storage type: $storageType")
        }
    }
}
