package gov.cdc.ocio.processingstatusapi.integration

import com.google.gson.Gson

import org.json.JSONObject
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MainTest {
    private fun isValidUUID(uuidString: String) :Boolean {
        return try {
            UUID.fromString(uuidString)
            true
        }catch (e:Exception){
            false
        }
    }
    @Test
    fun sendUploadMetadataVerifyReportToServiceBus(){

    }

    @Test
    fun sendUploadMetadataVerifyReportToProcessingStatusAPI(){
        val uploadReport = Upload(MetadataVersion.v2, UploadReportTypes.metadataVerify).createReport()
        //convert upload report to json
        val uploadReportAsJson = Gson().toJson(uploadReport)
        val uuid = UUID.randomUUID()

        val endPoint = "$processingStatusBaseURL/api/report/json/uploadId/$uuid?stageName=dex-metadata-verify&destinationId=dex-testing&eventType=test-event1"

        val responseObject = main.sendReportToProcessingStatusAPI(endPoint,uploadReportAsJson)
        val responseCode = responseObject.statusCode
        val responseBody = responseObject.responseBody


        val responseBodyAsJson = JSONObject(responseBody)

        val stageName = responseBodyAsJson.get("stage_name")
        val stageReportId =responseBodyAsJson.get("stage_report_id")


        assertEquals(200,responseCode,"Incorrect response code $responseCode. It should be 200.")
        assertEquals(stageName,"dex-metadata-verify", "Incorrect stage name")
        assertTrue(isValidUUID(stageReportId.toString()))

        //use the stageReportId to query the cosmosDB
        val reportFromCosmosDb = main.queryCosmosDB(stageReportId.toString(),"reportId")

        val cosmosDbPayloadContent = JSONObject(reportFromCosmosDb).get("content")

        if (cosmosDbPayloadContent!=null){
            assert(true)
        }

    }

    @Test
    fun sendReportWithMissingDestinationId(){
        val uploadReport = Upload(MetadataVersion.v2, UploadReportTypes.fileCopy).createReport()
        //convert upload report to json
        val uploadReportAsJson = Gson().toJson(uploadReport)
        val uuid = UUID.randomUUID()

        val endPoint = "$processingStatusBaseURL/api/report/json/uploadId/$uuid?stageName=dex-metadata-verify&eventType=test-event1"

        val responseObject = main.sendReportToProcessingStatusAPI(endPoint,uploadReportAsJson)
        val responseCode = responseObject.statusCode
        assertEquals(400,responseCode,"Incorrect response code for missing required destination_id")


    }
    @Test
    fun sendReportWithMissingUploadId(){
        val uploadReport = Upload(MetadataVersion.v2, UploadReportTypes.fileCopy).createReport()
        //convert upload report to json
        val uploadReportAsJson = Gson().toJson(uploadReport)

        val endPoint = "$processingStatusBaseURL/api/report/json/uploadId/stageName=dex-metadata-verify&eventType=test-event1"
        val responseObject = main.sendReportToProcessingStatusAPI(endPoint,uploadReportAsJson)
        val responseCode = responseObject.statusCode
        assertEquals(400,responseCode,"Incorrect response code for missing required uploadId")


    }
    @Test
    fun sendReportWithMissingEventType(){
        val uploadReport = Upload(MetadataVersion.v2, UploadReportTypes.metadataVerify).createReport()
        //convert upload report to json
        val uploadReportAsJson = Gson().toJson(uploadReport)
        val uuid = UUID.randomUUID()

        val endPoint = "$processingStatusBaseURL/api/report/json/uploadId/$uuid?stageName=dex-metadata-verify&destinationId=dex-testing"

        val responseObject = main.sendReportToProcessingStatusAPI(endPoint,uploadReportAsJson)
        val responseCode = responseObject.statusCode

        assertEquals(responseCode,400,"Incorrect response code for missing required eventType.")

    }
    @Test
    fun sendReportWithMissingStageName(){
        val uploadReport = Upload(MetadataVersion.v2, UploadReportTypes.metadataVerify).createReport()
        //convert upload report to json
        val uploadReportAsJson = Gson().toJson(uploadReport)
        val uuid = UUID.randomUUID()

        val endPoint = "$processingStatusBaseURL/api/report/json/uploadId/$uuid?destinationId=dex-testing&eventType=test-event1"

        val responseObject = main.sendReportToProcessingStatusAPI(endPoint,uploadReportAsJson)
        val responseCode = responseObject.statusCode
        assertEquals(400,responseCode,"Incorrect response code for missing required stageName.")

    }

    companion object {
        val main = IntegrationTest()
        private val processingStatusBaseURL = System.getenv("PROCESSING_STATUS_API_BASE_URL")

    }

}