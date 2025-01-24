
package gov.cdc.ocio.reportschemavalidator.health.schemaLoadersystem

import gov.cdc.ocio.types.health.HealthCheckResult
import gov.cdc.ocio.types.health.HealthCheckSystem
import gov.cdc.ocio.types.health.HealthStatusType
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.ListBucketsRequest


/**
 * Concrete implementation of the S3 Bucket health checks.
 */
class HealthCheckS3Bucket(
    system: String,
    private val getS3ClientFunc: () -> S3Client,
    private val s3Bucket: String,
) : HealthCheckSystem(system, "s3") {

    /**
     * Checks and sets S3 Bucket accessible status
     * @return HealthCheckResult
     */
    override fun doHealthCheck(): HealthCheckResult {
        val result = isS3FolderHealthy()
        result.onFailure { error ->
            val reason = "S3 bucket is not accessible and hence not healthy: ${error.localizedMessage}"
            logger.error(reason)
            return HealthCheckResult(system, service, HealthStatusType.STATUS_DOWN, reason)
        }
        return HealthCheckResult(system, service, HealthStatusType.STATUS_UP)
    }

    /**
     * Check whether S3 Buket is accessible
     *
     * @return Result<Boolean>
     */
    private fun isS3FolderHealthy(): Result<Boolean> {
        return try {
            val s3Client = getS3ClientFunc()
            val request = ListBucketsRequest.builder()
                .build()
            val response = s3Client.listBuckets(request)
            s3Client.close()
            if (response.buckets().any { it.name() == s3Bucket })
                Result.success(true)
            else
                Result.failure(Exception("Established connection to S3, but failed to verify the expected bucket exists."))
        } catch (e: Exception) {
            Result.failure(Exception("Failed to establish connection to S3 bucket: ${e.localizedMessage}"))
        }
    }
}
