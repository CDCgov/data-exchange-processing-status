package gov.cdc.ocio.reportschemavalidator.health.schemaLoadersystem.clientFactory


import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client


object S3ClientFactory {
    fun createClient(region: String):S3Client
    {
        return S3Client.builder().region(Region.of(region)).build()
    }
}