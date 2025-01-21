package gov.cdc.ocio.reportschemavalidator.schema

import com.azure.storage.blob.BlobClientBuilder
import com.azure.storage.blob.BlobServiceClientBuilder
import com.fasterxml.jackson.databind.ObjectMapper
import gov.cdc.ocio.reportschemavalidator.health.schemaLoadersystem.HealthCheckBlobContainer
import gov.cdc.ocio.reportschemavalidator.models.ReportSchemaMetadata
import gov.cdc.ocio.reportschemavalidator.models.SchemaLoaderInfo
import gov.cdc.ocio.reportschemavalidator.utils.DefaultJsonUtils
import gov.cdc.ocio.types.health.HealthCheckSystem


class BlobStorageSchemaClient(
    private val connectionString: String,
    private val containerName: String
) : SchemaStorageClient {

    private val blobServiceClient = BlobServiceClientBuilder()
        .connectionString(connectionString)
        .buildClient()

    private val containerClient = blobServiceClient
        .getBlobContainerClient(containerName)

    override fun getSchemaFile(fileName: String): String {
        val blobClient = BlobClientBuilder()
            .connectionString(connectionString)
            .containerName(containerName)
            .blobName(fileName)
            .buildClient()

        return blobClient.openInputStream().readAllBytes().decodeToString()
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
        return getSchemaContent("$schemaName.$schemaVersion.schema.json")
    }

    override var healthCheckSystem = HealthCheckBlobContainer(containerClient) as HealthCheckSystem
}
