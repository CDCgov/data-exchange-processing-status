package test

import com.google.gson.Gson
import com.microsoft.azure.functions.ExecutionContext
import gov.cdc.ocio.processingstatusapi.cosmos.CosmosContainerManager
import gov.cdc.ocio.processingstatusapi.exceptions.BadRequestException
import gov.cdc.ocio.processingstatusapi.functions.reports.ServiceBusProcessor
import gov.cdc.ocio.processingstatusapi.model.reports.CreateReportSBMessage
import gov.cdc.ocio.processingstatusapi.model.reports.stagereports.SchemaDefinition
import io.mockk.*
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

        mockkObject(CosmosContainerManager)

        every { CosmosContainerManager.initDatabaseContainer(any(), any()) } returns null
    }

    @Test
    fun testParseJsonContentSchemaDefinition() {
        val testMessage = File("./src/test/kotlin/data/service_bus_good_message.json").readText()

        val createReportSBMessage = Gson().fromJson(testMessage, CreateReportSBMessage::class.java)
        val schemaDefinition = SchemaDefinition.fromJsonString(createReportSBMessage.contentAsString)

        Assert.assertEquals(schemaDefinition.schemaName, "dex-hl7-validation")
        Assert.assertEquals(schemaDefinition.schemaVersion, "0.0.1")
    }

    @Test
    fun testServiceBusMessageMissingUploadId() {
        val testMessage = File("./src/test/kotlin/data/service_bus_missing_upload_id.json").readText()

        var exceptionThrown = false
        try {
            ServiceBusProcessor(context).withMessage(testMessage)
        } catch(ex: BadRequestException) {
            exceptionThrown = ex.localizedMessage == "Missing required field upload_id"
        }
        Assert.assertTrue(exceptionThrown)
    }

    @Test
    fun testServiceBusMessageMissingDestinationId() {
        val testMessage = File("./src/test/kotlin/data/service_bus_missing_destination_id.json").readText()

        var exceptionThrown = false
        try {
            ServiceBusProcessor(context).withMessage(testMessage)
        } catch(ex: BadRequestException) {
            exceptionThrown = ex.localizedMessage == "Missing required field destination_id"
        }
        Assert.assertTrue(exceptionThrown)
    }

    @Test
    fun testServiceBusMessageMissingEventType() {
        val testMessage = File("./src/test/kotlin/data/service_bus_missing_event_type.json").readText()

        var exceptionThrown = false
        try {
            ServiceBusProcessor(context).withMessage(testMessage)
        } catch(ex: BadRequestException) {
            exceptionThrown = ex.localizedMessage == "Missing required field event_type"
        }
        Assert.assertTrue(exceptionThrown)
    }

    @Test
    fun testServiceBusMessageMissingStageName() {
        val testMessage = File("./src/test/kotlin/data/service_bus_missing_stage_name.json").readText()

        var exceptionThrown = false
        try {
            ServiceBusProcessor(context).withMessage(testMessage)
        } catch(ex: BadRequestException) {
            exceptionThrown = ex.localizedMessage == "Missing required field stage_name"
        }
        Assert.assertTrue(exceptionThrown)
    }

    @Test
    fun testServiceBusMessageMissingContentType() {
        val testMessage = File("./src/test/kotlin/data/service_bus_missing_content_type.json").readText()

        var exceptionThrown = false
        try {
            ServiceBusProcessor(context).withMessage(testMessage)
        } catch(ex: BadRequestException) {
            exceptionThrown = ex.localizedMessage == "Missing required field content_type"
        }
        Assert.assertTrue(exceptionThrown)
    }

    @Test
    fun testServiceBusMessageMissingContent() {
        val testMessage = File("./src/test/kotlin/data/service_bus_missing_content.json").readText()

        var exceptionThrown = false
        try {
            ServiceBusProcessor(context).withMessage(testMessage)
        } catch(ex: BadRequestException) {
            exceptionThrown = ex.localizedMessage == "Missing required field content"
        }
        Assert.assertTrue(exceptionThrown)
    }

    @Test
    fun testServiceBusMessageContentMissingSchemaName() {
        val testMessage = File("./src/test/kotlin/data/service_bus_content_missing_schema_name.json").readText()

        var exceptionThrown = false
        try {
            ServiceBusProcessor(context).withMessage(testMessage)
        } catch(ex: BadRequestException) {
            exceptionThrown = ex.localizedMessage == "Invalid schema definition: Invalid schema_name provided"
        }
        Assert.assertTrue(exceptionThrown)
    }

    @Test
    fun testServiceBusMessageContentMissingSchemaVersion() {
        val testMessage = File("./src/test/kotlin/data/service_bus_content_missing_schema_version.json").readText()

        var exceptionThrown = false
        try {
            ServiceBusProcessor(context).withMessage(testMessage)
        } catch(ex: BadRequestException) {
            exceptionThrown = ex.localizedMessage == "Invalid schema definition: Invalid schema_version provided"
        }
        Assert.assertTrue(exceptionThrown)
    }

    //@Test
    fun testServiceBusGoodMessage() {
        val testMessage = File("./src/test/kotlin/data/service_bus_good_message.json").readText()
        ServiceBusProcessor(context).withMessage(testMessage)
    }

    @Test
    fun testServiceBusMessageEscapeQuotedJson() {
        val testMessage = File("./src/test/kotlin/data/service_bus_escape_quoted_json.json").readText()

        var exceptionThrown = false
        try {
            ServiceBusProcessor(context).withMessage(testMessage)
        } catch(ex: BadRequestException) {
            exceptionThrown = ex.localizedMessage == "content_type indicates json, but the content is not in JSON format"
        }
        Assert.assertTrue(exceptionThrown)
    }

}