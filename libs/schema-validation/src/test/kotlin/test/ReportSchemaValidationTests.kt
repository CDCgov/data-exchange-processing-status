
package test

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import gov.cdc.ocio.reportschemavalidator.errors.ErrorLoggerProcessor
import gov.cdc.ocio.reportschemavalidator.errors.ErrorProcessor
import gov.cdc.ocio.reportschemavalidator.loaders.FileSchemaLoader
import gov.cdc.ocio.reportschemavalidator.loaders.SchemaLoader
import gov.cdc.ocio.reportschemavalidator.models.ValidationSchemaResult
import gov.cdc.ocio.reportschemavalidator.service.SchemaValidationService
import gov.cdc.ocio.reportschemavalidator.utils.DefaultJsonUtils
import gov.cdc.ocio.reportschemavalidator.utils.JsonUtils
import gov.cdc.ocio.reportschemavalidator.validators.JsonSchemaValidator
import gov.cdc.ocio.reportschemavalidator.validators.SchemaValidator
import mu.KLogger
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import org.testng.Assert
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import java.io.File

class ReportSchemaValidationTests {

    private val objectMapper: ObjectMapper = jacksonObjectMapper()
    // Mock the KLogger dependency
    private val logger: KLogger = mock(KLogger::class.java)
    // Create the real instance of ErrorLoggerProcessor, injecting the mocked logger
    private val errorProcessor: ErrorProcessor = ErrorLoggerProcessor(logger)
    // Mock the schemaValidator dependency
    private val schemaValidator: SchemaValidator = JsonSchemaValidator(logger)
    //  Mock the jsonUtils dependency
    private val jsonUtils: JsonUtils = DefaultJsonUtils(objectMapper)
    // Mock the schemaValidator dependency
    private val schemaLoader: SchemaLoader = FileSchemaLoader()
    //Base validation failure reason
    private var reason = "The report could not be validated against the JSON schema: base.1.0.0.schema.json."

    private lateinit var schemaValidationService: SchemaValidationService

    @BeforeMethod
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        schemaValidationService = SchemaValidationService(
            schemaLoader,
            schemaValidator,
            errorProcessor,
            jsonUtils,
            logger
        )
    }

    @Test
    fun testReportSchemaValidationMissingUploadId() {
        val testMessage = File("./src/test/kotlin/data/report_schema_missing_uploadId_validation.json").readBytes()
        val message = createMessageFromBinary(testMessage)
        val result: ValidationSchemaResult = schemaValidationService.validateJsonSchema(message)
        Assert.assertTrue(!result.status)
        Assert.assertEquals(result.reason,reason)
        Assert.assertNotSame(result.invalidData, mutableListOf<String>())
        Assert.assertTrue(result.invalidData.any { it == "$.upload_id: is missing but it is required" })
    }
    @Test
    fun testReportSchemaValidationMissingDataStreamId() {
        val testMessage =File("./src/test/kotlin/data/report_schema_missing_dataStreamId_validation.json").readBytes()
        val message = createMessageFromBinary(testMessage)
        val result: ValidationSchemaResult = schemaValidationService.validateJsonSchema(message)
        Assert.assertTrue(!result.status)
        Assert.assertEquals(result.reason,reason)
        Assert.assertNotSame(result.invalidData, mutableListOf<String>())
        Assert.assertTrue(result.invalidData.any { it == "$.data_stream_id: is missing but it is required" })
    }
    @Test
    fun testReportSchemaValidationMissingRoute() {
        val testMessage =File("./src/test/kotlin/data/report_schema_missing_dataStreamRoute_validation.json").readBytes()
        val message = createMessageFromBinary(testMessage)
        val result: ValidationSchemaResult = schemaValidationService.validateJsonSchema(message)
        Assert.assertTrue(!result.status)
        Assert.assertEquals(result.reason,reason)
        Assert.assertNotSame(result.invalidData, mutableListOf<String>())
        Assert.assertTrue(result.invalidData.any { it == "$.data_stream_route: is missing but it is required" })
    }
    @Test
    fun testReportSchemaValidationMissingStageInfo() {
        val testMessage =File("./src/test/kotlin/data/report_schema_missing_stageInfo_validation.json").readBytes()
        val message = createMessageFromBinary(testMessage)
        val result: ValidationSchemaResult = schemaValidationService.validateJsonSchema(message)
        Assert.assertTrue(!result.status)
        Assert.assertEquals(result.reason,reason)
        Assert.assertNotSame(result.invalidData, mutableListOf<String>())
        Assert.assertTrue(result.invalidData.any { it == "$.stage_info: is missing but it is required" })
    }

    @Test
    fun testReportSchemaValidationMissingContentType() {
        val testMessage =File("./src/test/kotlin/data/report_schema_missing_contentType_validation.json").readBytes()
        val message = createMessageFromBinary(testMessage)
        val result: ValidationSchemaResult = schemaValidationService.validateJsonSchema(message)
        Assert.assertTrue(!result.status)
        Assert.assertEquals(result.reason,reason)
        Assert.assertNotSame(result.invalidData, mutableListOf<String>())
        Assert.assertTrue(result.invalidData.any { it == "$.content_type: is missing but it is required" })

    }

    @Test
    fun testReportSchemaValidationMissingContent() {
        val testMessage =File("./src/test/kotlin/data/report_schema_missing_content_validation.json").readBytes()
        val message = createMessageFromBinary(testMessage)
        val result: ValidationSchemaResult = schemaValidationService.validateJsonSchema(message)
        val missingContent ="Report rejected: `content` is not JSON or is missing."
        Assert.assertTrue(!result.status)
        Assert.assertEquals(result.reason,missingContent)
        Assert.assertNotSame(result.invalidData, mutableListOf<String>())
        Assert.assertTrue(result.invalidData.any { it == missingContent})
    }

    @Test
    fun testReportSchemaValidationMissingSchemaName() {
        val testMessage =File("./src/test/kotlin/data/report_schema_missing_schemaName_validation.json").readBytes()
        val message = createMessageFromBinary(testMessage)
        val result: ValidationSchemaResult = schemaValidationService.validateJsonSchema(message)
        val missingContent ="Report rejected: `content_schema_name` or `content_schema_version` is missing or empty."
        Assert.assertTrue(!result.status)
        Assert.assertEquals(result.reason,missingContent)
        Assert.assertNotSame(result.invalidData, mutableListOf<String>())
        Assert.assertTrue(result.invalidData.any { it == missingContent})

    }

    @Test
    fun testReportSchemaValidationMissingSchemaVersion() {
        val testMessage =File("./src/test/kotlin/data/report_schema_missing_schemaVersion_validation.json").readBytes()
        val message = createMessageFromBinary(testMessage)
        val result: ValidationSchemaResult = schemaValidationService.validateJsonSchema(message)
        val missingContent ="Report rejected: `content_schema_name` or `content_schema_version` is missing or empty."
        Assert.assertTrue(!result.status)
        Assert.assertEquals(result.reason,missingContent)
        Assert.assertNotSame(result.invalidData, mutableListOf<String>())
        Assert.assertTrue(result.invalidData.any { it == missingContent})
    }
    @Test
    fun testReportContentSchemaValidationFileNotFound() {
        val testMessage =File("./src/test/kotlin/data/report_schema_contentSchemaVersion_validation.json").readBytes()
        val message = createMessageFromBinary(testMessage)
        val result: ValidationSchemaResult = schemaValidationService.validateJsonSchema(message)
        val missingContent ="Report rejected: Content schema file not found for content schema name 'hl7v2-debatch' and schema version '2.0.0'."
        Assert.assertTrue(!result.status)
        Assert.assertEquals(result.reason,missingContent)
        Assert.assertNotSame(result.invalidData, mutableListOf<String>())
        Assert.assertTrue(result.invalidData.any { it == missingContent})

    }

    @Test
    fun testReportSchemaValidationPass() {
        val testMessage =File("./src/test/kotlin/data/report_schema_validation_pass.json").readBytes()
        val message = createMessageFromBinary(testMessage)
        val result: ValidationSchemaResult = schemaValidationService.validateJsonSchema(message)
        Assert.assertTrue(result.status)
        Assert.assertTrue(result.invalidData.isEmpty())
    }

    private fun createMessageFromBinary(messageBody: ByteArray): String {
        // var message = mockk<String>(relaxed = true)
        val message = messageBody.toString(Charsets.UTF_8)
        return message
    }
}




