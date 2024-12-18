package gov.cdc.ocio.reportschemavalidator.schema

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import java.io.InputStream

class S3SchemaStorageClient(private val bucketName: String, private val region: String) : SchemaStorageClient {

    private val s3Client: S3Client = S3Client.builder()
        .region(software.amazon.awssdk.regions.Region.of(region))
        .credentialsProvider(DefaultCredentialsProvider.create())
        .build()

    override fun getSchemaFile(schemaName: String): InputStream {
        val getObjectRequest = GetObjectRequest.builder()
            .bucket(bucketName)
            .key(schemaName)
            .build()

        return s3Client.getObject(getObjectRequest)
    }
}
