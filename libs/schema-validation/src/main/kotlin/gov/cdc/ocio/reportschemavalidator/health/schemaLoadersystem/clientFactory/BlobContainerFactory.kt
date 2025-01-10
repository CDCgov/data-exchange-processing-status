package gov.cdc.ocio.reportschemavalidator.health.schemaLoadersystem.clientFactory

import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.blob.BlobContainerClientBuilder

object BlobContainerFactory {
    fun createClient(connectionString: String,
                     container: String): BlobContainerClient {

        return BlobContainerClientBuilder()
            .connectionString(connectionString)
            .containerName(container)
            .buildClient()
    }
}