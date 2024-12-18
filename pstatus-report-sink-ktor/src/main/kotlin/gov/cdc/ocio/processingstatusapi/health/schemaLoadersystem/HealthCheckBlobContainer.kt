package gov.cdc.ocio.processingstatusapi.health.schemaLoadersystem


import com.azure.storage.blob.BlobContainerClientBuilder
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import gov.cdc.ocio.processingstatusapi.exceptions.BadStateException
import gov.cdc.ocio.processingstatusapi.plugins.AzureConfiguration
import gov.cdc.ocio.types.health.HealthCheckSystem
import gov.cdc.ocio.types.health.HealthStatusType
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


/**
 * Concrete implementation of the blob container health checks.
 */
@JsonIgnoreProperties("koin")
class HealthCheckBlobContainer : HealthCheckSystem("blob_storage"), KoinComponent {

    private val azureConfiguration by inject<AzureConfiguration>()

    /**
     * Checks and sets blob container accessible status
     *
     * @return HealthCheckBlobContainer object with updated status
     */
    override fun doHealthCheck() {
        try {
            if (isAzureBlobContainerHealthy(azureConfiguration)) {
                status = HealthStatusType.STATUS_UP
            }

        } catch (ex: Exception) {
            logger.error("Blob container is not accessible and hence not healthy $ex.message")
            healthIssues = ex.message
        }
    }

    /**
     * Check whether blob container is accessible.
     *
     * @return Boolean
     */
    @Throws(BadStateException::class)
    fun isAzureBlobContainerHealthy(config:AzureConfiguration): Boolean {
        return try {
            val blobContainerClient = BlobContainerClientBuilder()
                .connectionString(config.blobStorageConnectionString)
                .containerName(config.container)
                .buildClient()

            // Attempt to list blobs to ensure access
            val blobs = blobContainerClient.listBlobs().iterator()
            blobs.hasNext()
        } catch (e: Exception) {
            throw Exception("Failed to establish connection to blob storage.")
        }
    }

}