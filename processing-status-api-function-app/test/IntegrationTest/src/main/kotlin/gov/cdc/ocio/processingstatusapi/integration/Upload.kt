package gov.cdc.ocio.processingstatusapi.integration

import com.google.gson.annotations.SerializedName


enum class UploadReportTypes {
    MetadataVerify,
    FileCopy,
    UploadStatus
}

data class DexMetadataVerify(
    @SerializedName("schema_version") val schemaVersion: String,
    @SerializedName("schema_name") val schemaName: String,
    val filename: String,
    val timestamp: String,
    val metadata: Any,
    val issues: List<String>
)
data class DexUploadStatus(
    @SerializedName("schema_name") val schemaName: String,
    @SerializedName("schema_version") val schemaVersion: String,
    val tguid: String,
    val offset: Int,
    val size: Int,
    val filename: String,
    @SerializedName("meta_destination_id") val metaDestinationId: String,
    @SerializedName("meta_ext_event") val metaExtEvent: String,
    @SerializedName("end_time_epoch_millis") val endTimeEpochMillis: Long,
    @SerializedName("start_time_epoch_millis") val startTimeEpochMillis: Long,
    val metadata: Any
)



class Upload(metadataVersion: MetadataVersion, private val uploadReportType: UploadReportTypes) : ReportFactory<Any>(metadataVersion) {
    companion object {
        const val RECEIVED_FILENAME = "text/plain"
    }
    override fun createReport(): Any {
        return when (uploadReportType){
            UploadReportTypes.FileCopy -> createFileCopyReport()
            UploadReportTypes.UploadStatus -> createUploadStatusReport()
            UploadReportTypes.MetadataVerify -> createMetadataVerifyReport(metadataVersion)
        }

    }

    private fun createMetadataVerifyReport(metadataVersion: MetadataVersion): DexMetadataVerify {

        return when (metadataVersion) {
            MetadataVersion.V1 -> DexMetadataVerify("0.0.1",
                "dex-metadata-verify",
                "testfile1",
                "",
                DexMetadataVersionOne("testfile1",
                    RECEIVED_FILENAME,
                    "ndlp",
                    "routineImmunization",
                    "IZGW",
                    "V2022-07-28",
                    "DD2",
                  ),
                listOf("Missing required metadata field, 'meta_field1'.","Metadata field, 'meta_field2' is set to 'value3' and does not contain one of the allowed values: [ 'value1', value2' ]"))

            MetadataVersion.V2 -> DexMetadataVerify("0.0.1",
                "dex-metadata-verify",
                "testfile2",
                "",
                DexMetadataVersionTwo("testfile2",
                    RECEIVED_FILENAME,
                    "ndlp",
                    "routineImmunization",
                    "IZGW",
                    "V2022-12-26",
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
            MetadataVersion.V1 -> DexUploadStatus("upload",
                "1.0",
                "{{uploadId}}",
                0,
                27472691,
                "some_upload1.csv",
                "ndlp",
                "routineImmunization",
                1700009141546,
                1700009137234,
                DexMetadataVersionOne("testfile2",
                    RECEIVED_FILENAME,
                    "ndlp",
                    "routineImmunization",
                    "IZGW",
                    "V2022-05-05",
                    "DD2"))

            MetadataVersion.V2 -> DexUploadStatus("upload",
                "1.0",
                "{{uploadId}}",
                0,
                27472691,
                "some_upload1.csv",
                "ndlp",
                "routineImmunization",
                1700009141546,
                1700009137234,
                DexMetadataVersionTwo("testfile",
                    RECEIVED_FILENAME,
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
