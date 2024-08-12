package gov.cdc.ocio.processingstatusapi.integration


enum class MetadataVersion {
    V1,
    V2
}



abstract class ReportFactory<T>(val metadataVersion: MetadataVersion) {
    abstract fun createReport(): T


}