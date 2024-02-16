package functions

import com.microsoft.azure.functions.ExecutionContext
import gov.cdc.ocio.exceptions.BadRequestException
import gov.cdc.ocio.exceptions.InvalidSchemaDefException
import gov.cdc.ocio.functions.servicebus.ReportsNotificationsSBQueueProcessor
import org.mockito.Mockito
import org.testng.Assert
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
    fun testServiceBusMessageMissingDestinationId() {
        val testMessage = File("./src/test/kotlin/functions/serviceMockData/sb_missing_destination_id.json").readText()

        var exceptionThrown = false
        try {
            ReportsNotificationsSBQueueProcessor(context).withMessage(testMessage)
        } catch(ex: BadRequestException) {
            exceptionThrown = ex.localizedMessage == "Missing required field destination_id"
        }
        Assert.assertTrue(exceptionThrown)
    }


    @Test(description = "Tests for missing eventType in report json")
    fun testServiceBusMessageMissingEventType() {
        val testMessage = File("./src/test/kotlin/functions/serviceMockData/sb_missing_event_type.json").readText()

        var exceptionThrown = false
        try {
            ReportsNotificationsSBQueueProcessor(context).withMessage(testMessage)
        } catch(ex: BadRequestException) {
            exceptionThrown = ex.localizedMessage == "Missing required field event_type"
        }
        Assert.assertTrue(exceptionThrown)
    }

    @Test(description = "Tests for missing stageName in report json")
    fun testServiceBusMessageMissingStageName() {
        val testMessage = File("./src/test/kotlin/functions/serviceMockData/sb_missing_stage_name.json").readText()

        var exceptionThrown = false
        try {
            ReportsNotificationsSBQueueProcessor(context).withMessage(testMessage)
        } catch(ex: BadRequestException) {
            exceptionThrown = ex.localizedMessage == "Missing required field stage_name"
        }
        Assert.assertTrue(exceptionThrown)
    }

    @Test(description = "Tests for missing content_type in report json")
    fun testServiceBusMessageMissingContentType() {
        val testMessage = File("./src/test/kotlin/functions/serviceMockData/sb_missing_content_type.json").readText()

        var exceptionThrown = false
        try {
            ReportsNotificationsSBQueueProcessor(context).withMessage(testMessage)
        } catch(ex: BadRequestException) {
            exceptionThrown = ex.localizedMessage == "Missing required field content_type"
        }
        Assert.assertTrue(exceptionThrown)
    }

    @Test(description = "Tests for missing content in report json")
    fun testServiceBusMessageMissingContent() {
        val testMessage = File("./src/test/kotlin/functions/serviceMockData/sb_missing_content.json").readText()

        var exceptionThrown = false
        try {
            ReportsNotificationsSBQueueProcessor(context).withMessage(testMessage)
        } catch(ex: BadRequestException) {
            exceptionThrown = ex.localizedMessage == "Missing required field content"
        }
        Assert.assertTrue(exceptionThrown)
    }

    @Test(description = "Test for content format ")
    fun testServiceBusMessageEscapeQuotedJson() {
        val testMessage = File("./src/test/kotlin/functions/serviceMockData/sb_escape_quoted_json.json").readText()

        var exceptionThrown = false
        try {
            ReportsNotificationsSBQueueProcessor(context).withMessage(testMessage)
        } catch(ex: BadRequestException) {
            exceptionThrown = ex.localizedMessage == "content_type indicates json, but the content is not in JSON format"
        }
        Assert.assertTrue(exceptionThrown)
    }

    @Test(description = "Test for missing schema name")
    fun testServiceBusMessageContentMissingSchemaName() {
        val testMessage = File("./src/test/kotlin/functions/serviceMockData/sb_content_missing_schema_name.json").readText()

        var exceptionThrown = false
        try {
            ReportsNotificationsSBQueueProcessor(context).withMessage(testMessage)
        } catch(ex: InvalidSchemaDefException) {
            exceptionThrown = ex.localizedMessage == "Invalid schema_name provided"
        }
        Assert.assertTrue(exceptionThrown)
    }

    @Test(description = "Test for missing schema version")
    fun testServiceBusMessageContentMissingSchemaVersion() {
        val testMessage = File("./src/test/kotlin/functions/serviceMockData/sb_content_missing_schema_version.json").readText()

        var exceptionThrown = false
        try {
            ReportsNotificationsSBQueueProcessor(context).withMessage(testMessage)
        } catch(ex: InvalidSchemaDefException) {
            exceptionThrown = ex.localizedMessage == "Invalid schema_version provided"
        }
        Assert.assertTrue(exceptionThrown)
    }


    @Test(description = "Test for valid json content format in hL7 report with all 'SUCCESS' status")
    fun testValidJL7ReportWithSuccess() {
        val testMessage = File("./src/test/kotlin/functions/serviceMockData/hl7/sb_good_message_hl7_report_with_success.json").readText()
        val status = ReportsNotificationsSBQueueProcessor(context).withMessage(testMessage)
        Assert.assertEquals(status, "success")
    }

    @Test(description = "Test for valid json content format in hL7 report with few 'WARNING' status")
    fun testValidJL7ReportWithWarning() {
        val testMessage = File("./src/test/kotlin/functions/serviceMockData/hl7/sb_good_message_hl7_report_with_warning.json").readText()
        val status = ReportsNotificationsSBQueueProcessor(context).withMessage(testMessage)
        Assert.assertEquals(status, "warning")
    }

    @Test(description = "Test for valid json content format in hL7 report with all mixed ('WARNING','SUCCESS' & 'FAILURE') status")
    fun testValidJL7ReportWithFailure() {
        val testMessage = File("./src/test/kotlin/functions/serviceMockData/hl7/sb_good_message_hl7_report_with_failure.json").readText()
        val status = ReportsNotificationsSBQueueProcessor(context).withMessage(testMessage)
        Assert.assertEquals(status, "failure")
    }

     // File Copy Report Tests
    @Test(description = "Tests for validating 'success' status from file copy report")
    fun testValidFileCopyReportWithStatusSuccess() {
        val testMessage = File("./src/test/kotlin/functions/serviceMockData/fileCopy/sb_file_copy_report_success.json").readText()
         val status = ReportsNotificationsSBQueueProcessor(context).withMessage(testMessage)
         Assert.assertEquals(status, "success")
    }

    @Test(description = "Tests for validating 'failure' status from file copy report")
    fun testValidFileCopyReportWithStatusFailure() {
        val testMessage = File("./src/test/kotlin/functions/serviceMockData/fileCopy/sb_file_copy_report_failure.json").readText()
        val status = ReportsNotificationsSBQueueProcessor(context).withMessage(testMessage)
        Assert.assertEquals(status, "failure")
    }
}