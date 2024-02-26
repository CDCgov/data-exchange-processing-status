package gov.cdc.ocio.processingstatusapi.integration

class ReceiverDebatcher( metadataVersion: MetadataVersion,private val messageType: String, private val isBatch: Boolean = false) :
    ReportFactory( metadataVersion) {
    override fun createReport(): Map<String, String> {
        // Implementation for creating report
        return createReceiverDebatcherReport()

    }


}
private fun createReceiverDebatcherReport(): Map<String,String>{
    return mapOf()
}


class Redactor(metadataVersion: MetadataVersion,private val messageType: String) :
    ReportFactory(metadataVersion) {
    override fun createReport(): Map<String, String> {
        return createRedactorReport()
    }

}
private fun createRedactorReport(): Map<String,String>{
    return mapOf()
}


class StructureValidator(metadataVersion: MetadataVersion, private val messageType: String, private val structureErrors: Boolean = false, private val exceptions: Boolean = false) :
    ReportFactory(metadataVersion) {
    override fun createReport(): Map<String, String> {
        return createStructureValidatorReport()
    }


}
private fun createStructureValidatorReport(): Map<String,String>{
    return mapOf()
}

class JsonLake( metadataVersion: MetadataVersion) : ReportFactory(metadataVersion) {
    override fun createReport(): Map<String, String> {
       return createJsonLakeReport()
    }

}
private fun createJsonLakeReport(): Map<String, String> {
    return mapOf()
}

class LakeOfSegments( metadataVersion: MetadataVersion) : ReportFactory(metadataVersion) {
    override fun createReport(): Map<String, String> {
        return createLakeOfSegmentsReport()
    }


}
private fun createLakeOfSegmentsReport(): Map<String, String> {
    return mapOf()
}