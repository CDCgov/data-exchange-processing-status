package gov.cdc.ocio.reportschemavalidator.schema

import com.azure.storage.blob.BlobClientBuilder
import com.azure.storage.blob.BlobServiceClientBuilder
import com.fasterxml.jackson.databind.ObjectMapper
import gov.cdc.ocio.reportschemavalidator.models.ReportSchemaMetadata
import gov.cdc.ocio.reportschemavalidator.models.SchemaLoaderInfo
import gov.cdc.ocio.reportschemavalidator.utils.DefaultJsonUtils
import java.io.InputStream


class BlobStorageSchemaClient(
    private val connectionString: String,
    private val containerName: String
) : SchemaStorageClient {

    private val blobServiceClient = BlobServiceClientBuilder()
        .connectionString(connectionString)
        .buildClient()

    private val containerClient = blobServiceClient
        .getBlobContainerClient(containerName)

    override fun getSchemaFile(fileName: String): InputStream {
        val blobClient = BlobClientBuilder()
            .connectionString(connectionString)
            .containerName(containerName)
            .blobName(fileName)
            .buildClient()

        return blobClient.openInputStream()
    }

    /**
     * Provides a list of the schema files that are available.
     *
     * @return List<[ReportSchemaMetadata]>
     */
    override fun getSchemaFiles() = containerClient.listBlobs().map { blobItem ->
        getSchemaFile(blobItem.name).use { inputStream ->
            ReportSchemaMetadata.from(blobItem.name, inputStream)
        }
    }

    /**
     * Provides the schema loader information.
     *
     * @return SchemaLoaderInfo
     */
    override fun getInfo() = SchemaLoaderInfo(
        type = "blob",
        location = containerClient.blobContainerUrl
    )

    /**
     * Get the report schema content from the provided information.
     *
     * @param schemaFilename [String]
     * @return [Map]<[String], [Any]>
     */
    override fun getSchemaContent(schemaFilename: String): Map<String, Any> {
        getSchemaFile(schemaFilename).use { inputStream ->
            val jsonContent = inputStream.readAllBytes().decodeToString()
            return DefaultJsonUtils(ObjectMapper()).getJsonMapOfContent(jsonContent)
        }
    }

    /**
     * Get the report schema content from the provided information.
     *
     * @param schemaName [String]
     * @param schemaVersion [String]
     * @return [Map]<[String], [Any]>
     */
    override fun getSchemaContent(schemaName: String, schemaVersion: String): Map<String, Any> {
        return getSchemaContent("$schemaName.$schemaVersion.schema.json")
    }
}
