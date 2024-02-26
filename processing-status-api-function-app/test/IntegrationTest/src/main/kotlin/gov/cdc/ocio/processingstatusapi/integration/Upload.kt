package gov.cdc.ocio.processingstatusapi.integration


enum class UploadReportTypes {
    metadataVerify,
    fileCopy,
    upload_status
}
/*
data class DexFileCopy(
    val schema_name: String,
    val schema_version: String,
    val file_source_blob_url: String,
    val file_destination_blob_url: String,
    val timestamp: String,
    val result: String,
    val error_description: String
)

 */

class Upload(metadataVersion: MetadataVersion, private val uploadReportType: UploadReportTypes) : ReportFactory(metadataVersion) {
    override fun createReport(): Map<String, Any> {
        return when (uploadReportType){
            UploadReportTypes.fileCopy -> createFileCopyReport()
            UploadReportTypes.upload_status -> createUploadStatusReport()
            UploadReportTypes.metadataVerify -> createMetadataVerifyReport()
        }

    }

    private fun createMetadataVerifyReport(): Map<String, Any> {
        return mapOf("schema_version" to "0.0.1",
            "schema_name" to "dex-metadata-verify",
            "filename" to "10MB-test-file",
            "timestamp" to "",
            "metadata" to mapOf(
                "filename" to "10MB-test-file",
                "filetype" to "text/plain",
                "meta_destination_id" to "ndlp",
                "meta_ext_event" to "routineImmunization",
                "meta_ext_source" to "IZGW",
                "meta_ext_sourceversion" to "V2022-12-31",
                "meta_ext_entity" to "DD2",
                "meta_username" to "ygj6@cdc.gov",
                "meta_ext_objectkey" to "2b18d70c-8559-11ee-b9d1-0242ac120002",
                "meta_ext_filename" to "10MB-test-file",
                "meta_ext_submissionperiod" to "1",
                "meta_field2" to "value3"
            ),
            "issues" to listOf(
                "Missing required metadata field, 'meta_field1'.",
                "Metadata field, 'meta_field2' is set to 'value3' and does not contain one of the allowed values: [ 'value1', value2' ]"
            ))
    }
    private fun createFileCopyReport(): Map<String,String>{
        return mapOf("schema_name" to "dex-file-copy",
            "schema_version" to "0.0.1",
            "file_source_blob_url" to "",
            "file_destination_blob_url" to "",
            "timestamp" to "",
            "result" to "success",
            "error_description" to "")
    }
    private fun createUploadStatusReport(): Map<String,Any>{
        return mapOf("schema_name" to "upload",
            "schema_version" to "1.0",
            "tguid" to "{{uploadId}}",
            "offset" to 0,
            "size" to 27472691,
            "filename" to "some_upload1.csv",
            "meta_destination_id" to "ndlp",
            "meta_ext_event" to "routineImmunization",
            "end_time_epoch_millis" to 1700009141546,
            "start_time_epoch_millis" to 1700009137234,
            "metadata" to mapOf(
                "filename" to "10MB-test-file",
                "filetype" to "text/plain",
                "meta_destination_id" to "ndlp",
                "meta_ext_event" to "routineImmunization",
                "meta_ext_source" to "IZGW",
                "meta_ext_sourceversion" to "V2022-12-31",
                "meta_ext_entity" to "DD2",
                "meta_username" to "ygj6@cdc.gov",
                "meta_ext_objectkey" to "2b18d70c-8559-11ee-b9d1-0242ac120002",
                "meta_ext_filename" to "10MB-test-file",
                "meta_ext_submissionperiod" to "1"
            ))
    }
}
