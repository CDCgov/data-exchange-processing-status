package gov.cdc.ocio.reportschemavalidator.utils

enum class SchemaLoaderSystemType(val value: String) {
    S3("s3"),
    BLOB_STORAGE("blob_storage"),
    FILE_SYSTEM("file_system")
}