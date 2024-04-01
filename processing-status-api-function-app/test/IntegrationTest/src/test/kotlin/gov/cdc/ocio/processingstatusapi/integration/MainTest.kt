package gov.cdc.ocio.processingstatusapi.integration

import com.google.gson.Gson

import org.json.JSONObject
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.File

import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@DisplayName("Integration Test for Processing Status API")
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
    @DisplayName("Send various reports through Service Bus")
    fun sendReportsToServiceBusAndVerifyCounts(){
        //initialize object for  batch of reports
        val messages: MutableList<String> = mutableListOf()
        val validationReports = File(RESOURCES_PATH)
        val uuid = UUID.randomUUID()

        val reports = validationReports.listFiles()

        reports?.forEach { file ->
            val message = JSONObject()
            val contentAsJson = JSONObject(file.readText())
            message.put("upload_id",uuid)
            message.put("destination_id","dex-testing")
            message.put("stage_name",STAGE_NAME)
            message.put("content_type","json")
            message.put("event_type","test-event1")
            message.put("content",contentAsJson)
            messages.add(message.toString())
        }
        // send the batch through service bus queue
        main.serviceBusSenderClient(messages)

        // use counts endpoint to verify number of reports
        val countsEndPoint = "$processingStatusBaseURL/api/report/counts/$uuid"
        val response = main.getReportFromProcessingStatusAPI(countsEndPoint)
        val responseAsJson = JSONObject(response.responseBody)

        val stages = responseAsJson.getJSONObject("stages")
        val count = stages.get(STAGE_NAME)

        assertEquals(200,response.statusCode,"Incorrect response code ${response.statusCode}. It should be 200.")
        assertEquals(4,count,"Incorrect count $count. it should be 4")

    }
    @Test
    @DisplayName("Upload Metadata Verify Report")
    fun postUploadMetadataVerifyReport(){
        val uploadReport = Upload(MetadataVersion.v2, UploadReportTypes.MetadataVerify).createReport()
        //convert upload report to json
        val uploadReportAsJson = Gson().toJson(uploadReport)
        val uuid = UUID.randomUUID()

        val endPointWithUploadMetadataVerify = "$processingStatusBaseURL/api/report/json/uploadId/$uuid?stageName=dex-metadata-verify&destinationId=dex-testing&eventType=test-event1"
        val metadataVerifyResponseObject = main.sendReportToProcessingStatusAPI(endPointWithUploadMetadataVerify,uploadReportAsJson, POST_METHOD)

        val metadataVerifyReportAsJson = JSONObject(metadataVerifyResponseObject.responseBody)
        val stageName = metadataVerifyReportAsJson.get("stage_name")
        val stageReportId =metadataVerifyReportAsJson.get("report_id")

        assertEquals(200,metadataVerifyResponseObject.statusCode,"Incorrect response code. It should be 200.")
        assertEquals(stageName,"dex-metadata-verify", "Incorrect stage name")
        assertTrue(isValidUUID(stageReportId.toString()))

        //use the stageReportId to query the cosmosDB
        val reportFromCosmosDb = main.queryCosmosDB(stageReportId.toString(), REPORT_ID)

        val cosmosDbPayloadContent = JSONObject(reportFromCosmosDb).get(CONTENT)
        if (cosmosDbPayloadContent!=null){
            assert(true)
        }
    }
    @Test
    @DisplayName("Upload File Copy Report")
    fun postUploadFileCopyReport(){
        val uploadReport = Upload(MetadataVersion.v2, UploadReportTypes.FileCopy).createReport()
        //convert upload report to json
        val uploadReportAsJson = Gson().toJson(uploadReport)
        val uuid = UUID.randomUUID()
        val endPointWithUploadFileCopy = "$processingStatusBaseURL/api/report/json/uploadId/$uuid?stageName=dex-file-copy&dataStreamId=dex-testing&dataStreamRoute=test-event1"

        val uploadFileCopyResponseObject = main.sendReportToProcessingStatusAPI(endPointWithUploadFileCopy,uploadReportAsJson, POST_METHOD)
        val responseBodyAsJson = JSONObject(uploadFileCopyResponseObject.responseBody)
        val stageName = responseBodyAsJson.get("stage_name")
        val stageReportId =responseBodyAsJson.get("report_id")

        assertEquals(200,uploadFileCopyResponseObject.statusCode,"Incorrect response code. It should be 200.")
        assertEquals(stageName,"dex-file-copy", "Incorrect stage name, should be dex-file-copy")
        assertTrue(isValidUUID(stageReportId.toString()))

        //use the stageReportId to query the cosmosDB
        val reportFromCosmosDb = main.queryCosmosDB(stageReportId.toString(), REPORT_ID)
        val cosmosDbPayloadContent = JSONObject(reportFromCosmosDb).get(CONTENT)

        if (cosmosDbPayloadContent!=null){
            assert(true)
        }
    }
    @Test
    @DisplayName("Upload Status Report")
    fun postUploadStatusReportReport(){
        val uploadReport = Upload(MetadataVersion.v2, UploadReportTypes.UploadStatus).createReport()
        //convert upload report to json
        val uploadReportAsJson = Gson().toJson(uploadReport)
        val uuid = UUID.randomUUID()
        val endPointWithUploadStatus = "$processingStatusBaseURL/api/report/json/uploadId/$uuid?stageName=dex-upload-status&dataStreamId=dex-testing&dataStreamRoute=test-event1"

        val uploadStatusResponseObject = main.sendReportToProcessingStatusAPI(endPointWithUploadStatus,uploadReportAsJson, POST_METHOD)
        val responseBodyAsJson = JSONObject(uploadStatusResponseObject.responseBody)
        val stageName = responseBodyAsJson.get("stage_name")
        val stageReportId =responseBodyAsJson.get("report_id")

        assertEquals(200,uploadStatusResponseObject.statusCode,"Incorrect response code. It should be 200.")
        assertEquals(stageName,"dex-upload-status", "Incorrect stage name, should be dex-upload-status")
        assertTrue(isValidUUID(stageReportId.toString()))

        //use the stageReportId to query the cosmosDB
        val reportFromCosmosDb = main.queryCosmosDB(stageReportId.toString(), REPORT_ID)
        val cosmosDbPayloadContent = JSONObject(reportFromCosmosDb).get(CONTENT)

        if (cosmosDbPayloadContent!=null){
            assert(true)
        }

    }
    @Test
    @DisplayName("Routing File Copy Report")
    fun postRoutingFileCopyReport(){
        val routingFileCopyReport = Routing(MetadataVersion.v2, RoutingReportTypes.fileCopy).createReport()
        //convert report to json
        val reportAsJson = Gson().toJson(routingFileCopyReport)

        val uuid = UUID.randomUUID()
        val routingFileCopyEndPoint = "$processingStatusBaseURL/api/report/json/uploadId/$uuid?stageName=dex-routing&dataStreamId=dex-testing&dataStreamRoute=test-event1"

        val fileCopyResponseObject = main.sendReportToProcessingStatusAPI(routingFileCopyEndPoint,reportAsJson, POST_METHOD)

        val responseBodyAsJson = JSONObject(fileCopyResponseObject.responseBody)
        val stageName = responseBodyAsJson.get("stage_name")
        val stageReportId =responseBodyAsJson.get("report_id")

        assertEquals(200,fileCopyResponseObject.statusCode,"Incorrect response code. It should be 200.")
        assertEquals(stageName,"dex-routing", "Incorrect stage name, should be dex-routing")
        assertTrue(isValidUUID(stageReportId.toString()))

        //use the stageReportId to query the cosmosDB
        val reportFromCosmosDb = main.queryCosmosDB(stageReportId.toString(), REPORT_ID)
        val cosmosDbPayloadContent = JSONObject(reportFromCosmosDb).get(CONTENT)

        if (cosmosDbPayloadContent!=null){
            assert(true)
        }

    }
    @Test
    @DisplayName("HL7v2 Reports")
    fun postHl7ValidationReports(){
        val validationReports = File(RESOURCES_PATH)

        val uuid = UUID.randomUUID()
        val reports = validationReports.listFiles()

        reports?.forEach { file->
            val jsonString = file.readText()

            val endPointWithValidationReport = "$processingStatusBaseURL/api/report/json/uploadId/$uuid?stageName=$STAGE_NAME&dataStreamId=dex-testing&dataStreamRoute=test-event1"
            val validationResponseObject = main.sendReportToProcessingStatusAPI(endPointWithValidationReport,jsonString, POST_METHOD)

            val validationReportAsJson = JSONObject(validationResponseObject.responseBody)
            val stageName = validationReportAsJson.get("stage_name")
            val stageReportId =validationReportAsJson.get("report_id")

            assertEquals(200,validationResponseObject.statusCode,"Incorrect response code. It should be 200.")
            assertEquals(stageName,STAGE_NAME, "Incorrect stage name, should be dex-hl7")
            assertTrue(isValidUUID(stageReportId.toString()))
        }
        //final step: use counts endpoint to verify report count
        val countsEndpoint = "$processingStatusBaseURL/api/report/counts/$uuid"
        val responseObject = main.getReportFromProcessingStatusAPI(countsEndpoint)
        val responseBodyAsJson = JSONObject(responseObject.responseBody)

        val stages = responseBodyAsJson.getJSONObject("stages")
        val count = stages.get(STAGE_NAME)
        assertEquals(200,responseObject.statusCode,"Incorrect response code ${responseObject.statusCode}. It should be 200.")
        assertEquals(4,count,"Incorrect count $count. it should be 4")
    }

    @Test
    @DisplayName("Update Upload Metadata Verify Report")
    fun putUploadMetadataVerifyReport(){
        //step1: create report
        val uploadReport = Upload(MetadataVersion.v2, UploadReportTypes.MetadataVerify).createReport()
        //convert upload report to json
        val uploadReportAsJson = Gson().toJson(uploadReport)
        val uuid = UUID.randomUUID()

        //step2: Send the report to processing status api and verify response
        val endPointWithVerifyMetadataReport = "$processingStatusBaseURL/api/report/json/uploadId/$uuid?stageName=dex-metadata-verify&dataStreamId=dex-testing&dataStreamRoute=test-event1"
        val responseObject = main.sendReportToProcessingStatusAPI(endPointWithVerifyMetadataReport,uploadReportAsJson, POST_METHOD)
        val responseCode = responseObject.statusCode
        val responseBody = responseObject.responseBody

        val responseBodyAsJson = JSONObject(responseBody)
        val stageReportId =responseBodyAsJson.get("report_id")

        assertEquals(200,responseCode,"Incorrect response code $responseCode. It should be 200.")
        assertTrue(isValidUUID(stageReportId.toString()))

        //step3: using stageReportId, update existing report with stageName.
        val updateReportEndpoint = "$processingStatusBaseURL/api/report/json/uploadId/$uuid?stageName=dex-metadata-verify1&destinationId=dex-testing&eventType=dex-test-event1"
        val updateResponseObject = main.sendReportToProcessingStatusAPI(updateReportEndpoint,uploadReportAsJson, PUT_METHOD)
        val updateResponseCode = updateResponseObject.statusCode
        val updateResponseBody = updateResponseObject.responseBody
        val updateResponseBodyAsJson = JSONObject(updateResponseBody)
        val stageName = updateResponseBodyAsJson.get("stage_name")
        val updateStageReportId =updateResponseBodyAsJson.get("report_id")

        assertEquals(200,updateResponseCode,"Incorrect response code $responseCode. It should be 200.")
        assertEquals(stageName,"dex-metadata-verify1", "Incorrect stage name")
        assertTrue(isValidUUID(updateStageReportId.toString()))
    }
    @Test
    @DisplayName("Get existing File Copy Report")
    fun getUploadFileCopyReport(){
        //step1: create report
        val uploadReport = Upload(MetadataVersion.v2, UploadReportTypes.FileCopy).createReport()
        //convert upload report to json
        val uploadReportAsJson = Gson().toJson(uploadReport)
        val uuid = UUID.randomUUID()

        //step2: Send the report to processing status api and verify response
        val getUploadFileCopyEndPoint = "$processingStatusBaseURL/api/report/json/uploadId/$uuid?stageName=dex-metadata-verify&dataStreamId=dex-testing&dataStreamRoute=test-event1"
        val fileCopyresponseObject = main.sendReportToProcessingStatusAPI(getUploadFileCopyEndPoint,uploadReportAsJson, POST_METHOD)

        val fileCopyReportAsJson = JSONObject(fileCopyresponseObject.responseBody)
        val stageReportId =fileCopyReportAsJson.get("report_id")

        assertEquals(200,fileCopyresponseObject.statusCode,"Incorrect response code. It should be 200.")
        assertTrue(isValidUUID(stageReportId.toString()))

        //step3: GET upload file copy report from Processing Status API and verify response
        val getEndPoint = "$processingStatusBaseURL/api/report/uploadId/$uuid"
        val getResponseObject = main.getReportFromProcessingStatusAPI(getEndPoint)

        assertEquals(200,getResponseObject.statusCode, "Incorrect response code. It should be 200.")
        //validate the report received
        //by upload id
        val getResponseBodyAsJson = JSONObject(getResponseObject.responseBody)
        val uploadId = getResponseBodyAsJson.getString("upload_id")
        assertEquals(uuid.toString(), uploadId,"upload_id should be $uploadId")

        val reportsArray = getResponseBodyAsJson.getJSONArray("reports")
        val reportObject = reportsArray.getJSONObject(0)
        val reportId = reportObject.getString("report_id")
        val stageName = reportObject.getString("stage_name")

        //by reportId, stageName
        assertEquals(stageReportId, reportId, "reportId is incorrect, should be $stageReportId")
        assertEquals("dex-metadata-verify",stageName, "Incorrect stage_name should be dex-metadata-verify")
    }
    //Errors
    @Test
    @DisplayName("The report with missing destinationID")
    fun postReportWithMissingDestinationId(){
        val uploadReport = Upload(MetadataVersion.v2, UploadReportTypes.FileCopy).createReport()
        //convert upload report to json
        val uploadReportAsJson = Gson().toJson(uploadReport)
        val uuid = UUID.randomUUID()

        val endPointWithMissingDestinationId = "$processingStatusBaseURL/api/report/json/uploadId/$uuid?stageName=dex-metadata-verify&eventType=test-event1"

        val responseObject = main.sendReportToProcessingStatusAPI(endPointWithMissingDestinationId,uploadReportAsJson, POST_METHOD)
        assertEquals(400,responseObject.statusCode,"Incorrect response code for missing required destination_id")
    }
    @Test
    @DisplayName("The report with missing uploadId")
    fun postReportWithMissingUploadId(){
        val uploadReport = Upload(MetadataVersion.v2, UploadReportTypes.FileCopy).createReport()
        //convert upload report to json
        val uploadReportAsJson = Gson().toJson(uploadReport)

        val endPointMissingUploadId = "$processingStatusBaseURL/api/report/json/uploadId/stageName=dex-metadata-verify&eventType=test-event1"
        val responseObject = main.sendReportToProcessingStatusAPI(endPointMissingUploadId,uploadReportAsJson, POST_METHOD)
        //val responseBody = responseObject.responseBody
        assertEquals(400,responseObject.statusCode,"Incorrect response code for missing required uploadId")
    }
    @Test
    @DisplayName("The report with missing eventType")
    fun postReportWithMissingEventType(){
        val uploadReport = Upload(MetadataVersion.v2, UploadReportTypes.MetadataVerify).createReport()
        //convert upload report to json
        val uploadReportAsJson = Gson().toJson(uploadReport)
        val uuid = UUID.randomUUID()

        val endPointMissingEventType = "$processingStatusBaseURL/api/report/json/uploadId/$uuid?stageName=dex-metadata-verify&destinationId=dex-testing"

        val responseObject = main.sendReportToProcessingStatusAPI(endPointMissingEventType,uploadReportAsJson, POST_METHOD)

        assertEquals(responseObject.statusCode,400,"Incorrect response code for missing required eventType.")
    }
    @Test
    @DisplayName("The report with missing stageName")
    fun postReportWithMissingStageName(){
        val uploadReport = Upload(MetadataVersion.v2, UploadReportTypes.MetadataVerify).createReport()
        //convert upload report to json
        val uploadReportAsJson = Gson().toJson(uploadReport)
        val uuid = UUID.randomUUID()

        val endPointMissingStageName = "$processingStatusBaseURL/api/report/json/uploadId/$uuid?destinationId=dex-testing&eventType=test-event1"
        val responseObject = main.sendReportToProcessingStatusAPI(endPointMissingStageName,uploadReportAsJson, POST_METHOD)
        val responseCode = responseObject.statusCode
        assertEquals(400,responseCode,"Incorrect response code for missing required stageName.")
    }

    companion object {
        val main = IntegrationTest()
        private val processingStatusBaseURL = System.getenv("PROCESSING_STATUS_API_BASE_URL")
        private const val POST_METHOD = "POST"
        private const val PUT_METHOD = "PUT"
        private const val REPORT_ID = "reportId"
        private const val CONTENT = "content"
        private const val RESOURCES_PATH = "src/test/resources"
        private const val STAGE_NAME = "dex-hl7"
    }
}