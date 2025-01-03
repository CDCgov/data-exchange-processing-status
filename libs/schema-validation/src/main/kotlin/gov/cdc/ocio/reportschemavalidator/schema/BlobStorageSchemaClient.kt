package gov.cdc.ocio.reportschemavalidator.schema

import com.azure.storage.blob.BlobClientBuilder
import com.azure.storage.blob.BlobServiceClientBuilder
import gov.cdc.ocio.reportschemavalidator.models.ReportSchemaMetadata
import gov.cdc.ocio.reportschemavalidator.models.SchemaLoaderInfo
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

    override fun getSchemaFile(schemaName: String): InputStream {
        val blobClient = BlobClientBuilder()
            .connectionString(connectionString)
            .containerName(containerName)
            .blobName(schemaName)
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
}
