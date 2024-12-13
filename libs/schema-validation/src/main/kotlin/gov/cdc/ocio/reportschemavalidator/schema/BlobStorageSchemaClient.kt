package gov.cdc.ocio.reportschemavalidator.schema

import com.azure.storage.blob.BlobClientBuilder
import java.io.InputStream

class BlobStorageSchemaClient(private val connectionString: String, private val containerName: String) : SchemaStorageClient {

    override fun getSchemaFile(schemaName: String): InputStream {
        val blobClient = BlobClientBuilder()
            .connectionString(connectionString)
            .containerName(containerName)
            .blobName(schemaName)
            .buildClient()

        return blobClient.openInputStream()
    }
}
