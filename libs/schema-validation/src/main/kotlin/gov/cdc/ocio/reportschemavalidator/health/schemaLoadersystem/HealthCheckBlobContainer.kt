
package gov.cdc.ocio.reportschemavalidator.health.schemaLoadersystem

import com.azure.storage.blob.BlobContainerClient
import gov.cdc.ocio.types.health.HealthCheckResult
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
     * @return HealthCheckResult
     */
    override fun doHealthCheck(): HealthCheckResult {
        val result = isAzureBlobContainerHealthy()
        result.onFailure { error ->
            val reason = "Blob container is not accessible and hence not healthy ${error.localizedMessage}"
            logger.error(reason)
            return HealthCheckResult(service, HealthStatusType.STATUS_DOWN, reason)
        }
        return HealthCheckResult(service, HealthStatusType.STATUS_UP)
    }

    /**
     * Check whether blob container is accessible.
     *
     * @return Result<Boolean>
     */
    private fun isAzureBlobContainerHealthy(): Result<Boolean> {
        return try {
            // Attempt to list blobs to ensure access
            val blobs = blobContainerClient.listBlobs().iterator()
            if (blobs.hasNext())
                Result.success(true)
            else
                Result.failure(Exception("Established connection to blob storage, but failed list blobs check."))
        } catch (e: Exception) {
            throw Exception("Failed to establish connection to blob storage.")
        }
    }

}
