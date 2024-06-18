
package test

import com.google.gson.Gson
import com.microsoft.azure.functions.ExecutionContext
import gov.cdc.ocio.processingstatusapi.cosmos.CosmosContainerManager
import gov.cdc.ocio.processingstatusapi.exceptions.BadRequestException
import gov.cdc.ocio.processingstatusapi.models.reports.CreateReportSBMessage
import gov.cdc.ocio.processingstatusapi.models.reports.SchemaDefinition
import gov.cdc.ocio.processingstatusapi.plugins.ServiceBusProcessor
import io.mockk.*
import org.mockito.Mockito
import org.testng.Assert
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import java.io.File
import com.azure.messaging.servicebus.ServiceBusReceivedMessage

class ServiceBusTests {

    private lateinit var context: ExecutionContext

    @BeforeMethod
    fun setUp() {
        context = Mockito.mock(ExecutionContext::class.java)
        mockkObject(CosmosContainerManager)
        every { CosmosContainerManager.initDatabaseContainer(any(), any(),any(), any()) } returns null

    }
    @Test
    fun testParseJsonContentSchemaDefinition() {
        val testMessage = File("./src/test/kotlin/data/service_bus_good_message.json").readText()

        val createReportSBMessage = Gson().fromJson(testMessage, CreateReportSBMessage::class.java)
        val schemaDefinition = SchemaDefinition.fromJsonString(createReportSBMessage.content)

        Assert.assertEquals(schemaDefinition.schemaName, "dex-hl7-validation")
        Assert.assertEquals(schemaDefinition.schemaVersion, "0.0.1")
    }
    @Test
    fun testServiceBusMessageMissingUploadId() {
        val testMessage =File("./src/test/kotlin/data/service_bus_missing_upload_id.json").readText()
        val serviceBusReceivedMessage = createServiceBusReceivedMessageFromString(testMessage)
        var exceptionThrown = false
        try {
            // Mock file
            ServiceBusProcessor().withMessage(serviceBusReceivedMessage)
        } catch(ex: BadRequestException) {
            exceptionThrown = ex.localizedMessage == "Missing fields: uploadId"
        }
        Assert.assertTrue(exceptionThrown)
    }
    @Test
    fun testServiceBusMessageMissingDataStreamId() {
        val testMessage = File("./src/test/kotlin/data/service_bus_missing_data_stream_id.json").readText()
        val serviceBusReceivedMessage = createServiceBusReceivedMessageFromString(testMessage)
        var exceptionThrown = false
        try {
            ServiceBusProcessor().withMessage(serviceBusReceivedMessage)
        } catch(ex: BadRequestException) {
            exceptionThrown = ex.localizedMessage == "Missing fields: dataStreamId, content"
        }
        Assert.assertTrue(exceptionThrown)
    }

    @Test
    fun testServiceBusMessageMissingRoute() {
        val testMessage = File("./src/test/kotlin/data/service_bus_missing_data_stream_route.json").readText()
        val serviceBusReceivedMessage = createServiceBusReceivedMessageFromString(testMessage)
        var exceptionThrown = false
        try {
            ServiceBusProcessor().withMessage(serviceBusReceivedMessage)
        } catch(ex: BadRequestException) {
            exceptionThrown = ex.localizedMessage == "Missing fields: dataStreamRoute, content"
        }
        Assert.assertTrue(exceptionThrown)
    }
    @Test
    fun testServiceBusMessageMissingStageName() {
        val testMessage = File("./src/test/kotlin/data/service_bus_missing_stage_name.json").readText()
        val serviceBusReceivedMessage = createServiceBusReceivedMessageFromString(testMessage)

        var exceptionThrown = false
        try {
            ServiceBusProcessor().withMessage(serviceBusReceivedMessage)
        } catch(ex: BadRequestException) {
            exceptionThrown = ex.localizedMessage == "Missing fields: stageName, content"
        }
        Assert.assertTrue(exceptionThrown)
    }

    @Test
    fun testServiceBusMessageMissingContentType() {
        val testMessage = File("./src/test/kotlin/data/service_bus_missing_content_type.json").readText()
        val serviceBusReceivedMessage = createServiceBusReceivedMessageFromString(testMessage)
        var exceptionThrown = false
        try {
            ServiceBusProcessor().withMessage(serviceBusReceivedMessage)
        } catch(ex: BadRequestException) {
            exceptionThrown = ex.localizedMessage == "Missing fields: contentType, content"
        }
        Assert.assertTrue(exceptionThrown)
    }

    @Test
    fun testServiceBusMessageMissingContent() {
        val testMessage = File("./src/test/kotlin/data/service_bus_missing_content.json").readText()
        val serviceBusReceivedMessage = createServiceBusReceivedMessageFromString(testMessage)
        var exceptionThrown = false
        try {
            ServiceBusProcessor().withMessage(serviceBusReceivedMessage)
        } catch(ex: BadRequestException) {
            exceptionThrown = ex.localizedMessage == "Missing fields: content"
        }
        Assert.assertTrue(exceptionThrown)
    }

    @Test
    fun testServiceBusMessageContentMissingSchemaName() {
        val testMessage = File("./src/test/kotlin/data/service_bus_content_missing_schema_name.json").readText()
        val serviceBusReceivedMessage = createServiceBusReceivedMessageFromString(testMessage)
        var exceptionThrown = false
        try {
            ServiceBusProcessor().withMessage(serviceBusReceivedMessage)
        } catch(ex: BadRequestException) {
            exceptionThrown = ex.localizedMessage == "Invalid schema definition: Invalid schema_name provided"
        }
        Assert.assertTrue(exceptionThrown)
    }

    @Test
    fun testServiceBusMessageContentMissingSchemaVersion() {
        val testMessage = File("./src/test/kotlin/data/service_bus_content_missing_schema_version.json").readText()
        val serviceBusReceivedMessage = createServiceBusReceivedMessageFromString(testMessage)
        var exceptionThrown = false
        try {
            ServiceBusProcessor().withMessage(serviceBusReceivedMessage)
        } catch(ex: BadRequestException) {
            exceptionThrown = ex.localizedMessage == "Invalid schema definition: Invalid schema_version provided"
        }
        Assert.assertTrue(exceptionThrown)
    }
    @Test
    fun testServiceBusGoodMessage_V1() {
        val testMessage = File("./src/test/kotlin/data/service_bus_good_message_V1.json").readText()
        val serviceBusReceivedMessage = createServiceBusReceivedMessageFromString(testMessage)
        ServiceBusProcessor().withMessage(serviceBusReceivedMessage)
    }

    @Test
    fun testServiceBusGoodMessage() {
        val testMessage = File("./src/test/kotlin/data/service_bus_good_message.json").readText()
        val serviceBusReceivedMessage = createServiceBusReceivedMessageFromString(testMessage)
        ServiceBusProcessor().withMessage(serviceBusReceivedMessage)
    }

    @Test
    fun testServiceBusMessageEscapeQuotedJson() {
        val testMessage = File("./src/test/kotlin/data/service_bus_escape_quoted_json.json").readText()
        val serviceBusReceivedMessage = createServiceBusReceivedMessageFromString(testMessage)
        var exceptionThrown = false
        try {
            ServiceBusProcessor().withMessage(serviceBusReceivedMessage)
        } catch(ex: BadRequestException) {
            exceptionThrown = ex.localizedMessage == "Malformed message: class java.lang.String cannot be cast to class java.util.Map (java.lang.String and java.util.Map are in module java.base of loader 'bootstrap')"
        }
        Assert.assertTrue(exceptionThrown)
    }
}

fun createServiceBusReceivedMessageFromString(messageBody: String): ServiceBusReceivedMessage {
    val message = mockk<ServiceBusReceivedMessage>(relaxed = true)
    val messageId ="MessageId123"
    val status ="Active"
    every{ message.messageId } returns messageId
    every{ message.state.name } returns status
    every { message.body.toString() } returns messageBody
    return message
}


