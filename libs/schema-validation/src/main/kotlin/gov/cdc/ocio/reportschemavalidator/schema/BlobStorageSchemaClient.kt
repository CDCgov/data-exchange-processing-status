package gov.cdc.ocio.reportschemavalidator.schema

import com.azure.storage.blob.BlobClient
import com.azure.storage.blob.BlobClientBuilder
import com.azure.storage.blob.BlobServiceClientBuilder
import com.fasterxml.jackson.databind.ObjectMapper
import gov.cdc.ocio.reportschemavalidator.health.schemaLoadersystem.HealthCheckBlobContainer
import gov.cdc.ocio.reportschemavalidator.models.ReportSchemaMetadata
import gov.cdc.ocio.reportschemavalidator.models.SchemaLoaderInfo
import gov.cdc.ocio.reportschemavalidator.utils.DefaultJsonUtils
import gov.cdc.ocio.types.health.HealthCheckSystem
import java.io.FileNotFoundException
import java.nio.charset.StandardCharsets


class BlobStorageSchemaClient(
    system: String,
    private val connectionString: String,
    private val containerName: String
) : SchemaStorageClient {

    private val blobServiceClient = BlobServiceClientBuilder()
        .connectionString(connectionString)
        .buildClient()

    private val containerClient = blobServiceClient
        .getBlobContainerClient(containerName)

    override fun getSchemaFile(fileName: String): String {
        val blobClient = buildBlobClient(fileName)

        val result = runCatching {
            blobClient.openInputStream().readAllBytes().decodeToString()
        }

        return result.getOrThrow()
    }

    /**
     * Provides a list of the schema files that are available.
     *
     * @return List<[ReportSchemaMetadata]>
     */
    override fun getSchemaFiles() = containerClient.listBlobs().map { blobItem ->
        ReportSchemaMetadata.from(
            blobItem.name,
            getSchemaFile(blobItem.name)
        )
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
        val jsonContent = getSchemaFile(schemaFilename)
        return DefaultJsonUtils(ObjectMapper()).getJsonMapOfContent(jsonContent)
    }

    /**
     * Get the report schema content from the provided information.
     *
     * @param schemaName [String]
     * @param schemaVersion [String]
     * @return [Map]<[String], [Any]>
     */
    override fun getSchemaContent(schemaName: String, schemaVersion: String): Map<String, Any> {
        return getSchemaContent(getFilename(schemaName, schemaVersion))
    }

    /**
     * Upserts a report schema -- if it does not exist it is added, otherwise the schema is replaced.  The schema is
     * validated before it is allowed to be upserted.
     *
     * @param schemaName [String]
     * @param schemaVersion [String]
     * @param content [String]
     * @return [String] - filename of the upserted report schema
     */
    override fun upsertSchema(schemaName: String, schemaVersion: String, content: String): String {
        val schemaFilename = getFilename(schemaName, schemaVersion)
        val blobClient = buildBlobClient(schemaFilename)

        // Convert the schema content to a byte array and upload
        val data = content.toByteArray(StandardCharsets.UTF_8)
        blobClient.blockBlobClient.upload(data.inputStream(), data.size.toLong(), true)

        return schemaFilename
    }

    /**
     * Removes the schema file associated with the name and version provided.
     *
     * @param schemaName [String]
     * @param schemaVersion [String]
     * @return [String] - filename of the removed report schema
     */
    override fun removeSchema(schemaName: String, schemaVersion: String): String {
        val schemaFilename = getFilename(schemaName, schemaVersion)
        val blobClient = buildBlobClient(schemaFilename)

        val result = runCatching {
            // Delete the blob
            blobClient.delete()
        }
        result.onFailure {
            throw FileNotFoundException("Schema file not found or could not be deleted: "
                    + "$schemaFilename for schema: $schemaName, schemaVersion: $schemaVersion")
        }

        return schemaFilename
    }

    /**
     * Convenience function to build the blob client.
     *
     * @param blobName String
     * @return BlobClient
     */
    private fun buildBlobClient(blobName: String): BlobClient {
        return BlobClientBuilder()
            .connectionString(connectionString)
            .containerName(containerName)
            .blobName(blobName)
            .buildClient()
    }

    override var healthCheckSystem = HealthCheckBlobContainer(system, containerClient) as HealthCheckSystem
}
