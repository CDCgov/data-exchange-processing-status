
package gov.cdc.ocio.reportschemavalidator.health.schemaLoadersystem

import com.azure.storage.blob.BlobContainerClient
import gov.cdc.ocio.types.health.HealthCheckSystem
import gov.cdc.ocio.types.health.HealthStatusType


/**
 * Concrete implementation of the blob container health checks.
 */
class HealthCheckBlobContainer(
    private val blobContainerClient: BlobContainerClient
) : HealthCheckSystem("blob_storage") {

    /**
     * Checks and sets blob container accessible status
     *
     * @return HealthCheckBlobContainer object with updated status
     */
    override fun doHealthCheck() {
        try {
            if (isAzureBlobContainerHealthy()) {
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
    @Throws(Exception::class)
    fun isAzureBlobContainerHealthy(): Boolean {
        return try {
            // Attempt to list blobs to ensure access
            val blobs = blobContainerClient.listBlobs().iterator()
            blobs.hasNext()
        } catch (e: Exception) {
            throw Exception("Failed to establish connection to blob storage.")
        }
    }

}
