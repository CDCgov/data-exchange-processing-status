package gov.cdc.ocio.processingstatusapi.integration


enum class UploadReportTypes {
    metadataVerify,
    fileCopy,
    upload_status
}
data class DexMetadata(
    val filename: String,
    val filetype: String,
    val meta_destination_id: String,
    val meta_ext_event: String,
    val meta_ext_source: String,
    val meta_ext_sourceversion: String,
    val meta_ext_entity: String,
    val meta_username: String,
    val meta_ext_objectkey: String,
    val meta_ext_filename: String,
    val meta_ext_submissionperiod: String,
    val meta_field2: String
)

data class DexUploadFileCopy(
    val schema_name: String,
    val schema_version: String,
    val file_source_blob_url: String,
    val file_destination_blob_url: String,
    val timestamp: String,
    val result: String,
    val error_description: String
)
data class DexMetadataVerify(
    val schema_version: String,
    val schema_name: String,
    val filename: String,
    val timestamp: String,
    val metadata: DexMetadata,
    val issues: List<String>
)
data class DexUploadStatus(
    val schema_name: String,
    val schema_version: String,
    val tguid: String,
    val offset: Int,
    val size: Int,
    val filename: String,
    val meta_destination_id: String,
    val meta_ext_event: String,
    val end_time_epoch_millis: Long,
    val start_time_epoch_millis: Long,
    val metadata: DexMetadata
)



class Upload(metadataVersion: MetadataVersion, private val uploadReportType: UploadReportTypes) : ReportFactory<Any>(metadataVersion) {
    override fun createReport(): Any {
        return when (uploadReportType){
            UploadReportTypes.fileCopy -> createFileCopyReport()
            UploadReportTypes.upload_status -> createUploadStatusReport()
            UploadReportTypes.metadataVerify -> createMetadataVerifyReport()
        }

    }

    private fun createMetadataVerifyReport(): DexMetadataVerify {
        return DexMetadataVerify("0.0.1",
            "dex-metadata-verify",
            "10MB-test-file",
            "",
            DexMetadata("10MB-test-file",
                "text/plain",
                "ndlp",
                "routineImmunization",
                "IZGW",
                "V2022-12-31",
                "DD2",
                "ygj6@cdc.gov",
                "2b18d70c-8559-11ee-b9d1-0242ac120002",
                "10MB-test-file",
                "1",
                "value3"),
            listOf("Missing required metadata field, 'meta_field1'.","Metadata field, 'meta_field2' is set to 'value3' and does not contain one of the allowed values: [ 'value1', value2' ]"))

    }
    private fun createFileCopyReport(): DexUploadFileCopy{
        return DexUploadFileCopy("dex-file-copy",
            "0.0.1",
            "",
            "",
            "",
            "success",
             "")
    }
    private fun createUploadStatusReport(): DexUploadStatus{

        return DexUploadStatus("upload",
            "1.0",
            "{{uploadId}}",
            0,
            27472691,
            "some_upload1.csv",
            "ndlp",
            "routineImmunization",
            1700009141546,
            1700009137234,
            DexMetadata("10MB-test-file",
                "text/plain",
                "ndlp",
                "routineImmunization",
                "IZGW",
                "V2022-12-31",
                "DD2",
                "ygj6@cdc.gov",
                "2b18d70c-8559-11ee-b9d1-0242ac120002",
                "10MB-test-file",
                "1",
                "value3"))
    }
}
