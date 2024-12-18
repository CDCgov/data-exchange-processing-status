package gov.cdc.ocio.processingstatusapi.health.schemaLoadersystem


import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import gov.cdc.ocio.processingstatusapi.exceptions.BadStateException
import gov.cdc.ocio.processingstatusapi.plugins.AWSConfiguration
import gov.cdc.ocio.types.health.HealthCheckSystem
import gov.cdc.ocio.types.health.HealthStatusType
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response

/**
 * Concrete implementation of the S3 Bucket health checks.
 */
@JsonIgnoreProperties("koin")
class HealthCheckS3Bucket : HealthCheckSystem("s3"), KoinComponent {

    private val awsServiceConfiguration by inject<AWSConfiguration>()

    /**
     * Checks and sets S3 Bucket accessible status
     *
     * @return HealthCheck S3 object with updated status
     */
    override fun doHealthCheck() {
        try {
            if (isS3FolderHealthy(awsServiceConfiguration)) {
                status = HealthStatusType.STATUS_UP
            }

        } catch (ex: Exception) {
            logger.error("S3 bucket is not accessible and hence not healthy $ex.message")
            healthIssues = ex.message
        }
    }

    /**
     * Check whether S3 Buket is accessible
     *
     * @return Boolean
     */
    @Throws(BadStateException::class)
    fun isS3FolderHealthy(config:AWSConfiguration): Boolean {
        return try {
            val s3Client = S3Client.builder().region(software.amazon.awssdk.regions.Region.of(config.s3Region)).build()
            val request = ListObjectsV2Request.builder()
                .bucket(config.s3Bucket)
                .maxKeys(1) // one file - lightweight check
                .build()
            val response: ListObjectsV2Response = s3Client.listObjectsV2(request)
            response.contents().isNotEmpty()
        } catch (e: Exception) {
            throw Exception("Failed to establish connection to S3 bucket.")
        }
    }
}