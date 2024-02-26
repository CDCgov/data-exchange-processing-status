package gov.cdc.ocio.processingstatusapi.integration


enum class MetadataVersion {
    v1,
    v2
}



abstract class ReportFactory(val metadataVersion: MetadataVersion) {
    abstract fun createReport(): Map<String,String>


}