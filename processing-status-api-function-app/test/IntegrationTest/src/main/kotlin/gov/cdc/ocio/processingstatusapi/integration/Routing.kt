package gov.cdc.ocio.processingstatusapi.integration


enum class RoutingReportTypes {
    fileCopy
}
class Routing(metadataVersion: MetadataVersion, private val routingReportType: RoutingReportTypes) : ReportFactory(metadataVersion) {
    override fun createReport(): Map<String, String> {
        // Implementation for creating report
        return when (routingReportType){
            RoutingReportTypes.fileCopy -> createFileCopyReport()
        }
    }

    private fun createFileCopyReport(): Map<String,String>{
        return mapOf()
    }

}