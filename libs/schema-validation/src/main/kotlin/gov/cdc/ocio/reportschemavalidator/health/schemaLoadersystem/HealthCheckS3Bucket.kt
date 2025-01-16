
package gov.cdc.ocio.reportschemavalidator.health.schemaLoadersystem

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import gov.cdc.ocio.reportschemavalidator.utils.AWSS3Configuration
import gov.cdc.ocio.types.health.HealthCheckResult
import gov.cdc.ocio.types.health.HealthCheckSystem
import gov.cdc.ocio.types.health.HealthStatusType
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request


/**
 * Concrete implementation of the S3 Bucket health checks.
 */
@JsonIgnoreProperties("koin")
class HealthCheckS3Bucket(private val s3Client: S3Client) : HealthCheckSystem("s3"), KoinComponent {

    private val awsServiceConfiguration by inject<AWSS3Configuration>()

    /**
     * Checks and sets S3 Bucket accessible status
     * @return HealthCheckResult
     */
    override fun doHealthCheck(): HealthCheckResult {
        val result = isS3FolderHealthy(awsServiceConfiguration)
        result.onFailure { error ->
            val reason = "S3 bucket is not accessible and hence not healthy ${error.localizedMessage}"
            logger.error(reason)
            return HealthCheckResult(service, HealthStatusType.STATUS_DOWN, reason)
        }
        return HealthCheckResult(service, HealthStatusType.STATUS_UP)
    }

    /**
     * Check whether S3 Buket is accessible
     *
     * @param config AWSS3Configuration
     * @return Result<Boolean>
     */
    private fun isS3FolderHealthy(config: AWSS3Configuration): Result<Boolean> {
        return try {
            val request = ListObjectsV2Request.builder()
                .bucket(config.s3Bucket)
                .maxKeys(1) // one file - lightweight check
                .build()
            val response = s3Client.listObjectsV2(request)
            if (response.contents().isNotEmpty())
                Result.success(true)
            else
                Result.failure(Exception("Established connection to S3 bucket, but failed list objects check."))
        } catch (e: Exception) {
            throw Exception("Failed to establish connection to S3 bucket.")
        }
    }
}
