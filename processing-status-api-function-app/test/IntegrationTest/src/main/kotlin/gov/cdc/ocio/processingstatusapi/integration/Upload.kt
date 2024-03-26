package gov.cdc.ocio.processingstatusapi.integration


enum class UploadReportTypes {
    MetadataVerify,
    FileCopy,
    UploadStatus
}

data class DexMetadataVerify(
    val schema_version: String,
    val schema_name: String,
    val filename: String,
    val timestamp: String,
    val metadata: Any,
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
    val metadata: Any
)



class Upload(metadataVersion: MetadataVersion, private val uploadReportType: UploadReportTypes) : ReportFactory<Any>(metadataVersion) {
    override fun createReport(): Any {
        return when (uploadReportType){
            UploadReportTypes.FileCopy -> createFileCopyReport()
            UploadReportTypes.UploadStatus -> createUploadStatusReport()
            UploadReportTypes.MetadataVerify -> createMetadataVerifyReport(metadataVersion)
        }

    }

    private fun createMetadataVerifyReport(metadataVersion: MetadataVersion): DexMetadataVerify {

        return when (metadataVersion) {
            MetadataVersion.v1 -> DexMetadataVerify("0.0.1",
                "dex-metadata-verify",
                "10MB-test-file",
                "",
                DexMetadataVersionOne("10MB-test-file",
                    "text/plain",
                    "ndlp",
                    "routineImmunization",
                    "IZGW",
                    "V2022-12-31",
                    "DD2",
                  ),
                listOf("Missing required metadata field, 'meta_field1'.","Metadata field, 'meta_field2' is set to 'value3' and does not contain one of the allowed values: [ 'value1', value2' ]"))

            MetadataVersion.v2 -> DexMetadataVerify("0.0.1",
                "dex-metadata-verify",
                "10MB-test-file",
                "",
                DexMetadataVersionTwo("10MB-test-file",
                    "text/plain",
                    "ndlp",
                    "routineImmunization",
                    "IZGW",
                    "V2022-12-31",
                    "DD2",
                    "ygj6@cdc.gov",
                    "2b18d70c-8559-11ee-b9d1-0242ac120002"),
                listOf("Missing required metadata field, 'meta_field1'.","Metadata field, 'meta_field2' is set to 'value3' and does not contain one of the allowed values: [ 'value1', value2' ]"))


        }


    }
    private fun createFileCopyReport(): DexFileCopy{
        return DexFileCopy("dex-file-copy",
            "0.0.1",
            "https://doesnotexist.blob.core.windows.net/integration-psapi/routeingress/13/13/13/1MB-test-file.txt",
            "https://doesnotexist.blob.core.windows.net/integration-psapi/13/13/13/1MB-test-file.txt",
            "",
            "success",
             "")
    }
    private fun createUploadStatusReport(): DexUploadStatus{

        return when (metadataVersion) {
            MetadataVersion.v1 -> DexUploadStatus("upload",
                "1.0",
                "{{uploadId}}",
                0,
                27472691,
                "some_upload1.csv",
                "ndlp",
                "routineImmunization",
                1700009141546,
                1700009137234,
                DexMetadataVersionOne("10MB-test-file",
                    "text/plain",
                    "ndlp",
                    "routineImmunization",
                    "IZGW",
                    "V2022-12-31",
                    "DD2"))

            MetadataVersion.v2 -> DexUploadStatus("upload",
                "1.0",
                "{{uploadId}}",
                0,
                27472691,
                "some_upload1.csv",
                "ndlp",
                "routineImmunization",
                1700009141546,
                1700009137234,
                DexMetadataVersionTwo("10MB-test-file",
                    "text/plain",
                    "ndlp",
                    "routineImmunization",
                    "IZGW",
                    "V2022-12-31",
                    "DD2",
                    "ygj6@cdc.gov",
                    "2b18d70c-8559-11ee-b9d1-0242ac120002"))
        }

    }
}
