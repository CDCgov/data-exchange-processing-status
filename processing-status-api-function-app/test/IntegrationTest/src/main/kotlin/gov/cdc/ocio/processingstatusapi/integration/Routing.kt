package gov.cdc.ocio.processingstatusapi.integration


enum class RoutingReportTypes {
    fileCopy
}

data class DexRoutingFileCopy(
    val schema_name: String,
    val schema_version: String,
    val file_source_blob_url: String,
    val file_destination_blob_url: String,
    val timestamp: String,
    val result: String,
    val error_description: String
)
class Routing(metadataVersion: MetadataVersion, private val routingReportType: RoutingReportTypes) : ReportFactory<Any>(metadataVersion) {
    override fun createReport(): Any {
        return when (routingReportType){
            RoutingReportTypes.fileCopy -> createFileCopyReport()
        }
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

}