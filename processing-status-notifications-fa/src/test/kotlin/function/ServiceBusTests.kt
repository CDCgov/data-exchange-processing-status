package function

import com.microsoft.azure.functions.ExecutionContext
import gov.cdc.ocio.exception.BadRequestException
import gov.cdc.ocio.exception.InvalidSchemaDefException
import gov.cdc.ocio.function.servicebus.ReportsNotificationsSBQueueProcessor
import org.mockito.Mockito
import org.testng.Assert.*
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import java.io.File


class ServiceBusTests {

    private lateinit var context: ExecutionContext

    @BeforeMethod
    fun setUp() {
        context = Mockito.mock(ExecutionContext::class.java)
    }

    @Test(description = "Tests for missing destination id in report json")
    fun testServiceBusMessageMissingdataStreamId() {
        val testMessage = File("./src/test/kotlin/function/serviceMockData/sb_missing_data_stream_id.json").readText()

        var exceptionThrown = false
        try {
            ReportsNotificationsSBQueueProcessor(context).withMessage(testMessage)
        } catch(ex: BadRequestException) {
            exceptionThrown = ex.localizedMessage == "Missing required field data_stream_id"
        }
        assertTrue(exceptionThrown)
    }


    @Test(description = "Tests for missing dataStreamRoute in report json")
    fun testServiceBusMessageMissingdataStreamRoute() {
        val testMessage = File("./src/test/kotlin/function/serviceMockData/sb_missing_data_stream_route.json").readText()

        var exceptionThrown = false
        try {
            ReportsNotificationsSBQueueProcessor(context).withMessage(testMessage)
        } catch(ex: BadRequestException) {
            exceptionThrown = ex.localizedMessage == "Missing required field data_stream_route"
        }
        assertTrue(exceptionThrown)
    }

    @Test(description = "Tests for missing stageName in report json")
    fun testServiceBusMessageMissingStageName() {
        val testMessage = File("./src/test/kotlin/function/serviceMockData/sb_missing_stage_name.json").readText()

        var exceptionThrown = false
        try {
            ReportsNotificationsSBQueueProcessor(context).withMessage(testMessage)
        } catch(ex: BadRequestException) {
            exceptionThrown = ex.localizedMessage == "Missing required field stage_name"
        }
        assertTrue(exceptionThrown)
    }

    @Test(description = "Tests for missing content_type in report json")
    fun testServiceBusMessageMissingContentType() {
        val testMessage = File("./src/test/kotlin/function/serviceMockData/sb_missing_content_type.json").readText()

        var exceptionThrown = false
        try {
            ReportsNotificationsSBQueueProcessor(context).withMessage(testMessage)
        } catch(ex: BadRequestException) {
            exceptionThrown = ex.localizedMessage == "Missing required field content_type"
        }
        assertTrue(exceptionThrown)
    }

    @Test(description = "Tests for missing content in report json")
    fun testServiceBusMessageMissingContent() {
        val testMessage = File("./src/test/kotlin/function/serviceMockData/sb_missing_content.json").readText()

        var exceptionThrown = false
        try {
            ReportsNotificationsSBQueueProcessor(context).withMessage(testMessage)
        } catch(ex: BadRequestException) {
            exceptionThrown = ex.localizedMessage == "Missing required field content"
        }
        assertTrue(exceptionThrown)
    }

    @Test(description = "Test for content format ")
    fun testServiceBusMessageEscapeQuotedJson() {
        val testMessage = File("./src/test/kotlin/function/serviceMockData/sb_escape_quoted_json.json").readText()

        var exceptionThrown = false
        try {
            ReportsNotificationsSBQueueProcessor(context).withMessage(testMessage)
        } catch(ex: BadRequestException) {
            exceptionThrown = ex.localizedMessage == "content_type indicates json, but the content is not in JSON format"
        }
        assertTrue(exceptionThrown)
    }

    @Test(description = "Test for missing schema name")
    fun testServiceBusMessageContentMissingSchemaName() {
        val testMessage = File("./src/test/kotlin/function/serviceMockData/sb_content_missing_schema_name.json").readText()

        var exceptionThrown = false
        try {
            ReportsNotificationsSBQueueProcessor(context).withMessage(testMessage)
        } catch(ex: InvalidSchemaDefException) {
            exceptionThrown = ex.localizedMessage == "Invalid schema_name provided"
        }
        assertTrue(exceptionThrown)
    }

    @Test(description = "Test for missing schema version")
    fun testServiceBusMessageContentMissingSchemaVersion() {
        val testMessage = File("./src/test/kotlin/function/serviceMockData/sb_content_missing_schema_version.json").readText()

        var exceptionThrown = false
        try {
            ReportsNotificationsSBQueueProcessor(context).withMessage(testMessage)
        } catch(ex: InvalidSchemaDefException) {
            exceptionThrown = ex.localizedMessage == "Invalid schema_version provided"
        }
        assertTrue(exceptionThrown)
    }

    // HL7 Report Tests
    @Test(description = "Test for valid json content format in hL7 report with all 'SUCCESS' status")
    fun testValidHL7ReportWithSuccess() {
        val testMessage = File("./src/test/kotlin/function/serviceMockData/hl7/sb_good_message_hl7_report_with_success.json").readText()
        val status = ReportsNotificationsSBQueueProcessor(context).withMessage(testMessage)
        assertEquals(status, "success")
    }

    @Test(description = "Test for valid json content format in hL7 report with few 'WARNING' status")
    fun testValidHL7ReportWithWarning() {
        val testMessage = File("./src/test/kotlin/function/serviceMockData/hl7/sb_good_message_hl7_report_with_warning.json").readText()
        val status = ReportsNotificationsSBQueueProcessor(context).withMessage(testMessage)
        assertEquals(status, "failure")
    }

    @Test(description = "Test for valid json content format in hL7 report with all mixed ('WARNING','SUCCESS' & 'FAILURE') status")
    fun testValidHL7ReportWithFailure() {
        val testMessage = File("./src/test/kotlin/function/serviceMockData/hl7/sb_good_message_hl7_report_with_failure.json").readText()
        val status = ReportsNotificationsSBQueueProcessor(context).withMessage(testMessage)
        assertEquals(status, "failure")
    }

     // File Copy Report Tests
    @Test(description = "Tests for validating 'success' status from file copy report")
    fun testValidFileCopyReportWithStatusSuccess() {
        val testMessage = File("./src/test/kotlin/function/serviceMockData/fileCopy/sb_good_message_file_copy_report_success.json").readText()
         val status = ReportsNotificationsSBQueueProcessor(context).withMessage(testMessage)
         assertEquals(status, "success")
    }

    @Test(description = "Tests for validating 'failure' status from file copy report")
    fun testValidFileCopyReportWithStatusFailure() {
        val testMessage = File("./src/test/kotlin/function/serviceMockData/fileCopy/sb_good_message_file_copy_report_failure.json").readText()
        val status = ReportsNotificationsSBQueueProcessor(context).withMessage(testMessage)
        assertEquals(status, "failure")
    }

    @Test(description = "Tests for checking invalidStatusType status from file copy report")
    fun testValidFileCopyReportWithStatusInvalid() {
        val testMessage = File("./src/test/kotlin/function/serviceMockData/fileCopy/sb_good_message_file_copy_report_invalid.json").readText()
        val status = ReportsNotificationsSBQueueProcessor(context).withMessage(testMessage)
        assertTrue(status != "success")
    }

    // Metadata Verify Test
    @Test(description = "Tests for validating 'success' status from metadata verify report")
    fun testValidMetadataVerifyReportWithStatusSuccess() {
        val testMessage = File("./src/test/kotlin/function/serviceMockData/metadataVerify/sb_good_message_metadata_verify_report_success.json").readText()
        val status = ReportsNotificationsSBQueueProcessor(context).withMessage(testMessage)
        assertEquals(status, "success")
    }

    @Test(description = "Tests for validating 'failure' status from metadata verify report")
    fun testValidMetadataVerifyReportWithStatusFailure() {
        val testMessage = File("./src/test/kotlin/function/serviceMockData/metadataVerify/sb_good_message_metadata_verify_report_failure.json").readText()
        val status = ReportsNotificationsSBQueueProcessor(context).withMessage(testMessage)
        assertEquals(status, "failure")
    }

}