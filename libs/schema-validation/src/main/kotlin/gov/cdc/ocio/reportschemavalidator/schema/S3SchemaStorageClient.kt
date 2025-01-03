package gov.cdc.ocio.reportschemavalidator.schema

import gov.cdc.ocio.reportschemavalidator.models.ReportSchemaMetadata
import gov.cdc.ocio.reportschemavalidator.models.SchemaLoaderInfo
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request
import java.io.InputStream
import java.util.function.Consumer


class S3SchemaStorageClient(private val bucketName: String, private val region: String) : SchemaStorageClient {

    private val s3Client = S3Client.builder()
        .region(Region.of(region))
        .credentialsProvider(DefaultCredentialsProvider.create())
        .build()

    override fun getSchemaFile(schemaName: String): InputStream {
        val getObjectRequest = GetObjectRequest.builder()
            .bucket(bucketName)
            .key(schemaName)
            .build()

        return s3Client.getObject(getObjectRequest)
    }

    /**
     * Provides a list of the schema files that are available.
     *
     * @return List<[ReportSchemaMetadata]>
     */
    override fun getSchemaFiles(): List<ReportSchemaMetadata> {
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
                val s3ObjectInputStream = s3Client.getObject(getObjectRequest)
                schemaFiles.add(ReportSchemaMetadata.from(s3Object.key(), s3ObjectInputStream))
            })
        }
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

}
