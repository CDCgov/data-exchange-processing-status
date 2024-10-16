
package test

import com.azure.messaging.servicebus.ServiceBusReceivedMessage
import com.microsoft.azure.functions.ExecutionContext
import gov.cdc.ocio.database.cosmos.CosmosContainerManager
import gov.cdc.ocio.processingstatusapi.plugins.ServiceBusProcessor
import io.mockk.*
import org.mockito.Mockito
import org.testng.Assert
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import java.io.File

class ReportSchemaValidationTests {

    private lateinit var context: ExecutionContext

    @BeforeMethod
    fun setUp() {
        context = Mockito.mock(ExecutionContext::class.java)
        mockkObject(CosmosContainerManager)
        every { CosmosContainerManager.initDatabaseContainer(any(), any(),any(), any()) } returns null

    }

    @Test
    fun testReportSchemaValidationMissingUploadId() {
        val testMessage =File("./src/test/kotlin/data/report_schema_missing_uploadId_validation.json").readBytes()
        val serviceBusReceivedMessage = createServiceBusReceivedMessageFromBinary(testMessage)
        var exceptionThrown = false
        try {
            // Mock file
            ServiceBusProcessor().withMessage(serviceBusReceivedMessage)
        } catch(ex: Exception) {
            exceptionThrown = ex.localizedMessage.contains("$.upload_id: is missing but it is required")
        }
        Assert.assertTrue(exceptionThrown)
    }
    @Test
    fun testReportSchemaValidationMissingDataStreamId() {
        val testMessage =File("./src/test/kotlin/data/report_schema_missing_dataStreamId_validation.json").readBytes()
        val serviceBusReceivedMessage = createServiceBusReceivedMessageFromBinary(testMessage)
        var exceptionThrown = false
        try {
            ServiceBusProcessor().withMessage(serviceBusReceivedMessage)
        } catch(ex: Exception) {
            exceptionThrown = ex.localizedMessage.contains("$.data_stream_id: is missing but it is required")
        }
        Assert.assertTrue(exceptionThrown)
    }
    @Test
    fun testReportSchemaValidationMissingRoute() {
        val testMessage =File("./src/test/kotlin/data/report_schema_missing_dataStreamRoute_validation.json").readBytes()
        val serviceBusReceivedMessage = createServiceBusReceivedMessageFromBinary(testMessage)
        var exceptionThrown = false
        try {
            ServiceBusProcessor().withMessage(serviceBusReceivedMessage)
        } catch(ex: Exception) {
            exceptionThrown = ex.localizedMessage.contains("$.data_stream_route: is missing but it is required")
        }
        Assert.assertTrue(exceptionThrown)
    }
    @Test
    fun testReportSchemaValidationMissingStageInfo() {
        val testMessage =File("./src/test/kotlin/data/report_schema_missing_stageInfo_validation.json").readBytes()
        val serviceBusReceivedMessage = createServiceBusReceivedMessageFromBinary(testMessage)

        var exceptionThrown = false
        try {
            ServiceBusProcessor().withMessage(serviceBusReceivedMessage)
        } catch(ex: Exception) {
            exceptionThrown = ex.localizedMessage.contains("$.stage_info: is missing but it is required")
        }
        Assert.assertTrue(exceptionThrown)
    }

    @Test
    fun testReportSchemaValidationMissingContentType() {
        val testMessage =File("./src/test/kotlin/data/report_schema_missing_contentType_validation.json").readBytes()
        val serviceBusReceivedMessage = createServiceBusReceivedMessageFromBinary(testMessage)
        var exceptionThrown = false
        try {
            ServiceBusProcessor().withMessage(serviceBusReceivedMessage)
        } catch(ex: Exception) {
            exceptionThrown = ex.localizedMessage.contains("$.content_type: is missing but it is required")
        }
        Assert.assertTrue(exceptionThrown)
    }

    @Test
    fun testReportSchemaValidationMissingContent() {
        val testMessage =File("./src/test/kotlin/data/report_schema_missing_content_validation.json").readBytes()
        val serviceBusReceivedMessage = createServiceBusReceivedMessageFromBinary(testMessage)
        var exceptionThrown = false
        try {
            ServiceBusProcessor().withMessage(serviceBusReceivedMessage)
        } catch(ex: Exception) {
            exceptionThrown = ex.localizedMessage.contains("Report rejected: `content` is not JSON or is missing.")
        }
        Assert.assertTrue(exceptionThrown)
    }

    @Test
    fun testReportSchemaValidationMissingSchemaName() {
        val testMessage =File("./src/test/kotlin/data/report_schema_missing_schemaName_validation.json").readBytes()
        val serviceBusReceivedMessage = createServiceBusReceivedMessageFromBinary(testMessage)
        var exceptionThrown = false
        try {
            ServiceBusProcessor().withMessage(serviceBusReceivedMessage)
        } catch(ex: Exception) {
            exceptionThrown = ex.localizedMessage.contains("Report rejected: `content_schema_name` or `content_schema_version` is missing or empty.")
        }
        Assert.assertTrue(exceptionThrown)
    }

    @Test
    fun testReportSchemaValidationMissingSchemaVersion() {
        val testMessage =File("./src/test/kotlin/data/report_schema_missing_schemaVersion_validation.json").readBytes()
        val serviceBusReceivedMessage = createServiceBusReceivedMessageFromBinary(testMessage)
        var exceptionThrown = false
        try {
            ServiceBusProcessor().withMessage(serviceBusReceivedMessage)
        } catch(ex: Exception) {
            exceptionThrown = ex.localizedMessage.contains("Report rejected: `content_schema_name` or `content_schema_version` is missing or empty.")
        }
        Assert.assertTrue(exceptionThrown)
    }
    @Test
    fun testReportContentSchemaValidationFileNotFound() {
        val testMessage =File("./src/test/kotlin/data/report_schema_contentSchemaVersion_validation.json").readBytes()
        val serviceBusReceivedMessage = createServiceBusReceivedMessageFromBinary(testMessage)
        var exceptionThrown = false
        try {
            ServiceBusProcessor().withMessage(serviceBusReceivedMessage)
        }
        catch (ex :IllegalArgumentException){
            exceptionThrown = ex.localizedMessage.contains("File not found: hl7v2-debatch.2.0.0.schema.json")
        }
        catch(ex: Exception) {
            exceptionThrown = ex.localizedMessage.contains("Report rejected: Content schema file not found for content schema name 'hl7v2-debatch' and schema version '2.0.0'.")
        }
        Assert.assertTrue(exceptionThrown)
    }

    @Test
    fun testReportSchemaValidationPass() {
        val testMessage =File("./src/test/kotlin/data/report_schema_validation_pass.json").readBytes()
        val serviceBusReceivedMessage = createServiceBusReceivedMessageFromBinary(testMessage)
        ServiceBusProcessor().withMessage(serviceBusReceivedMessage)
    }

    private fun createServiceBusReceivedMessageFromBinary(messageBody: ByteArray): ServiceBusReceivedMessage {
        val message = mockk<ServiceBusReceivedMessage>(relaxed = true)
        val messageId ="MessageId123"
        val status ="Active"
        every{ message.messageId } returns messageId
        every{ message.state.name } returns status
        every { message.body.toBytes() } returns messageBody
        return message
    }


}




