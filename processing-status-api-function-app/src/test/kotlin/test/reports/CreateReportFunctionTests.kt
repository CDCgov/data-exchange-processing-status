package test.reports

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.ocio.processingstatusapi.cosmos.CosmosContainerManager
import gov.cdc.ocio.processingstatusapi.functions.reports.CreateReportFunction
import gov.cdc.ocio.processingstatusapi.functions.reports.ReportManager
import gov.cdc.ocio.processingstatusapi.model.DispositionType
import gov.cdc.ocio.processingstatusapi.model.reports.Source
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.mockkStatic
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import utils.HttpResponseMessageMock
import java.io.File
import java.util.*


class CreateReportFunctionTests {

    private lateinit var request: HttpRequestMessage<Optional<String>>

    private lateinit var context: ExecutionContext

    var queryParameters= mutableMapOf<String, String?>()

    private val uploadId = "1"
    val testMessage = File("./src/test/kotlin/data/reports/createReport_badrequest.json").readText()


    @BeforeMethod
    fun setUp() {
        context = Mockito.mock(ExecutionContext::class.java)
        mockkObject(CosmosContainerManager)
        every { CosmosContainerManager.initDatabaseContainer(any(), any()) } returns null
        request = Mockito.mock(HttpRequestMessage::class.java) as HttpRequestMessage<Optional<String>>

        mockkStatic(ReportManager::class)
        // Setup method invocation interception when createResponseBuilder is called to avoid null pointer on real method call.
        Mockito.doAnswer { invocation ->
            val status = invocation.arguments[0] as HttpStatus
            HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status)
        }.`when`(request).createResponseBuilder(any())
    }

    @Test
    fun testWithUploadIDMissingDestinationId() {
        queryParameters= mutableMapOf<String, String?>()
        queryParameters["eventType"] = "1"
        queryParameters["stageName"] = "1"
        `when`(request.body).thenReturn(Optional.of(testMessage))
        `when` (request.queryParameters).thenReturn(queryParameters)
        val response = CreateReportFunction(request, DispositionType.ADD).jsonWithUploadId("1");
        //assert(response.body.toString() == "destinationId is required")
        assert(response.status == HttpStatus.BAD_REQUEST)
    }

    @Test
    fun testWithUploadIDMissingEventType() {
        queryParameters= mutableMapOf<String, String?>()
        queryParameters["destinationId"] = "1"
        queryParameters["stageName"] = "1"
        `when`(request.body).thenReturn(Optional.of(testMessage))
        `when` (request.queryParameters).thenReturn(queryParameters)
        val response = CreateReportFunction(request, DispositionType.ADD).jsonWithUploadId("1");
        //assert(response.body.toString() == "eventType is required")
        assert(response.status == HttpStatus.BAD_REQUEST)
    }

    @Test
    fun testWithUploadIDMissingStageName() {
        queryParameters= mutableMapOf<String, String?>()
        queryParameters["destinationId"] = "1"
        queryParameters["eventType"] = "1"
        `when`(request.body).thenReturn(Optional.of(testMessage))
        `when` (request.queryParameters).thenReturn(queryParameters)
        val response = CreateReportFunction(request, DispositionType.ADD).jsonWithUploadId("1");
        //assert(response.body.toString() == "stageName is required")
        assert(response.status == HttpStatus.BAD_REQUEST)
    }

    //@Test
    fun testWithUploadIDInvalidSchema() {
        queryParameters= mutableMapOf<String, String?>()
        queryParameters["destinationId"] = "1"
        queryParameters["eventType"] = "1"
        queryParameters["stageName"] = "1"
        `when`(request.body).thenReturn(Optional.of(testMessage))
        `when` (request.queryParameters).thenReturn(queryParameters)
         val response = CreateReportFunction(request, DispositionType.ADD).jsonWithUploadId(uploadId);
         assert(response.status == HttpStatus.BAD_REQUEST)
         //assert(response.body.toString() == "Invalid schema definition: Invalid schema_name provided")
    }

    //@Test
    fun testWithUploadID_validSchema() {
        queryParameters= mutableMapOf<String, String?>()
        queryParameters["destinationId"] = "1"
        queryParameters["eventType"] = "1"
        queryParameters["stageName"] = "1"
        `when`(request.body).thenReturn(Optional.of(testMessage))
        `when` (request.queryParameters).thenReturn(queryParameters)

        every{ ReportManager().createReportWithUploadId(any(), any(), any(), any(), any(), any(), DispositionType.ADD, Source.HTTP)} returns "1"
        val response = CreateReportFunction(request, DispositionType.ADD).jsonWithUploadId(uploadId);
        assert(response.status == HttpStatus.BAD_REQUEST)
        //assert(response.body.toString() == "Invalid schema definition: Invalid schema_name provided")
    }

}

