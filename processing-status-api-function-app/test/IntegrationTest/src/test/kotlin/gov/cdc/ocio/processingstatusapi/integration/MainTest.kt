package gov.cdc.ocio.processingstatusapi.integration

import org.junit.jupiter.api.Test

class MainTest {

    @Test
    fun uploadMetadataVerifyReport(){
        val main = IntegrationTest()

        val upload = Upload(MetadataVersion.v2, UploadReportTypes.metadataVerify)


    }
    @Test
    fun uploadMetadataVerifyReportWithIssues(){

    }
}