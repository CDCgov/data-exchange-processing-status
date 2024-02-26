package gov.cdc.ocio.processingstatusapi.integration


enum class UploadReportTypes {
    metadataVerify,
    fileCopy,
    upload_status
}

class Upload(metadataVersion: MetadataVersion, private val uploadReportType: UploadReportTypes) : ReportFactory(metadataVersion) {
    override fun createReport(): Map<String, String> {
        return when (uploadReportType){
            UploadReportTypes.fileCopy -> createFileCopyReport()
            UploadReportTypes.upload_status -> createUploadStatusReport()
            UploadReportTypes.metadataVerify -> createMetadataVerifyReport()
        }


    }

    private fun createMetadataVerifyReport(): Map<String,String>{
        return mapOf()
    }
    private fun createFileCopyReport(): Map<String,String>{
        return mapOf()
    }
    private fun createUploadStatusReport(): Map<String,String>{
        return mapOf()
    }
}
