package gov.cdc.ocio.reportschemavalidator.schema

import com.fasterxml.jackson.databind.ObjectMapper
import gov.cdc.ocio.reportschemavalidator.health.schemaLoadersystem.HealthCheckS3Bucket
import gov.cdc.ocio.reportschemavalidator.models.ReportSchemaMetadata
import gov.cdc.ocio.reportschemavalidator.models.SchemaLoaderInfo
import gov.cdc.ocio.reportschemavalidator.utils.DefaultJsonUtils
import gov.cdc.ocio.types.health.HealthCheckSystem
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.auth.credentials.WebIdentityTokenFileCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request
import java.nio.file.Path
import java.util.function.Consumer


class S3SchemaStorageClient(
    system: String,
    private val bucketName: String,
    private val region: String,
    private val roleArn: String?,
    private val webIdentityTokenFile: String?
) : SchemaStorageClient {

    /**
     * Create an S3 client using the region and credentials provider.
     *
     * @return S3Client
     */
    private fun getS3Client(): S3Client {

        val credentialsProvider = if (roleArn.isNullOrEmpty() ||
            webIdentityTokenFile.isNullOrEmpty()
        ) {
            // Fallback to default credentials provider (access key and secret)
            DefaultCredentialsProvider.create()
        } else {
            // Use Web Identity Token
            WebIdentityTokenFileCredentialsProvider.builder()
                .roleArn(roleArn)
                .webIdentityTokenFile(webIdentityTokenFile.let { Path.of(it) })
                .build()
        }

        // Load credentials from the AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY and AWS_SESSION_TOKEN environment variables.
        return S3Client.builder()
            .region(Region.of(region))
            .credentialsProvider(credentialsProvider)
            .build()
    }

    /**
     * Get a schema file from the provided filename.
     *
     * @param fileName String
     * @return String
     */
    override fun getSchemaFile(fileName: String): String {
        val s3Client = getS3Client()

        val getObjectRequest = GetObjectRequest.builder()
            .bucket(bucketName)
            .key(fileName)
            .build()

        val result = s3Client
            .getObject(getObjectRequest)
            .readAllBytes()
            .decodeToString()

        s3Client.close()

        return result
    }

    /**
     * Provides a list of the schema files that are available.
     *
     * @return List<[ReportSchemaMetadata]>
     */
    override fun getSchemaFiles(): List<ReportSchemaMetadata> {
        val s3Client = getS3Client()

        val request = ListObjectsV2Request.builder()
            .bucket(bucketName)
            .build()

        val response = s3Client.listObjectsV2Paginator(request)

        val schemaFiles = mutableListOf<ReportSchemaMetadata>()
        response.forEach { page ->
            page.contents().forEach(Consumer { s3Object ->
                val getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Object.key())
                    .build()
                val content = s3Client
                    .getObject(getObjectRequest)
                    .readAllBytes()
                    .decodeToString()
                schemaFiles.add(ReportSchemaMetadata.from(s3Object.key(), content))
            })
        }
        s3Client.close()

        return schemaFiles
    }

    /**
     * Provides the schema loader information.
     *
     * @return SchemaLoaderInfo
     */
    override fun getInfo() = SchemaLoaderInfo(
        type = "s3",
        location = "$bucketName.s3.$region.amazonaws.com"
    )

    /**
     * Get the report schema content from the provided information.
     *
     * @param schemaFilename [String]
     * @return [Map]<[String], [Any]>
     */
    override fun getSchemaContent(schemaFilename: String): Map<String, Any> {
        val s3Client = getS3Client()

        val request = GetObjectRequest.builder()
            .bucket(bucketName)
            .key(schemaFilename)
            .build()

        val response = runCatching {
            val response = s3Client.getObject(request)
            return@runCatching response.readAllBytes().decodeToString()
        }
        s3Client.close()

        return when (response.isSuccess) {
            true -> DefaultJsonUtils(ObjectMapper()).getJsonMapOfContent(response.getOrDefault(""))
            else -> mapOf("failure" to (response.exceptionOrNull()?.localizedMessage ?: "error"))
        }
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

    override var healthCheckSystem = HealthCheckS3Bucket(system, ::getS3Client, bucketName) as HealthCheckSystem
}
