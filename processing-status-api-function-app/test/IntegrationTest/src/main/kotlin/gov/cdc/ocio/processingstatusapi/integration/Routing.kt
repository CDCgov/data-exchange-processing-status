package gov.cdc.ocio.processingstatusapi.integration


enum class RoutingReportTypes {
    FileCopy
}
class Routing(metadataVersion: MetadataVersion, private val routingReportType: RoutingReportTypes) : ReportFactory<Any>(metadataVersion) {
    override fun createReport(): Any {
        return when (routingReportType){
            RoutingReportTypes.FileCopy -> createFileCopyReport()
        }
    }

    private fun createFileCopyReport(): DexFileCopy{
        return DexFileCopy("dex-file-copy",
            "0.0.1",
            "dne/edav/csv_file1_87847487844.csv",
            "dne/csv_file1.csv",
            "",
            "success",
            "")
    }

}