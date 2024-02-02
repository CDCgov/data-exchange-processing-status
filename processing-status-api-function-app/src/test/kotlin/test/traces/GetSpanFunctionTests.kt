package test.traces

import com.azure.cosmos.CosmosClient
import com.azure.cosmos.CosmosContainer
import com.azure.cosmos.CosmosDatabase
import com.azure.cosmos.util.CosmosPagedIterable
import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.ocio.processingstatusapi.cosmos.CosmosContainerManager
import gov.cdc.ocio.processingstatusapi.functions.traces.GetSpanFunction
import gov.cdc.ocio.processingstatusapi.model.reports.Report
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import khttp.responses.Response
import org.json.JSONObject
import org.mockito.Mockito
import org.mockito.Mockito.doAnswer
import org.mockito.kotlin.any
import org.testng.Assert.assertEquals
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import utils.HttpResponseMessageMock
import java.io.File
import java.util.*


class GetSpanFunctionTests {

    private lateinit var context: ExecutionContext
    private lateinit var request: HttpRequestMessage<Optional<String>>
    private val testBytes = File("./src/test/kotlin/data/trace/get_trace.json").readText()
    private val mockResponse = mockk<Response>()
    // Convert the string to a JSONObject
    private val jsonObject = JSONObject(testBytes)
    private val items = mockk<CosmosPagedIterable<Report>>()

    @BeforeMethod
    fun setUp() {
        context = Mockito.mock(ExecutionContext::class.java)
        request = Mockito.mock(HttpRequestMessage::class.java) as HttpRequestMessage<Optional<String>>
        val mockCosmosClient = mockk<CosmosClient>()
        val mockCosmosDb = mockk<CosmosDatabase>()
        val mockCosmosContainer = mockk<CosmosContainer>()
        mockkObject(CosmosContainerManager)
        every { CosmosContainerManager.initDatabaseContainer(any(), any()) } returns mockCosmosContainer
        every { mockCosmosClient.getDatabase(any()) } returns mockCosmosDb
        every { mockCosmosDb.getContainer(any()) } returns mockCosmosContainer
        every { mockCosmosContainer.queryItems(any<String>(), any(), Report::class.java) } returns items

        mockkStatic("khttp.KHttp")
        every {khttp.get(any())} returns mockResponse
        every {mockResponse.jsonObject} returns jsonObject
        // Setup method invocation interception when createResponseBuilder is called to avoid null pointer on real method call.
        doAnswer { invocation ->
            val status = invocation.arguments[0] as HttpStatus
            HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status)
        }.`when`(request).createResponseBuilder(any())
    }

    @Test
    fun testWithQueryParams_ok() {
        val getSpanFunction = GetSpanFunction(request)
        every {mockResponse.statusCode} returns HttpStatus.OK.value()

        val result = getSpanFunction.withQueryParams()
        assertEquals(400, result.statusCode)
    }

//    @Test
//    fun testWithTraceId_bad_request() {
//        val getStatusFunction = GetStatusFunction(request)
//        every {mockResponse.statusCode} returns HttpStatus.BAD_REQUEST.value()
//
//        val result = getStatusFunction.withTraceId("1")
//        assertEquals(400, result.statusCode)
//    }
//
//    @Test
//    fun testWithUploadId_ok() {
//        val getStatusFunction = GetStatusFunction(request)
//        every {mockResponse.statusCode} returns HttpStatus.OK.value()
//
//        val result = getStatusFunction.withUploadId("1")
//        assertEquals(200, result.statusCode)
//    }
//
//    @Test
//    fun testWithUploadId_bad_request() {
//        val getStatusFunction = GetStatusFunction(request)
//        every {mockResponse.statusCode} returns HttpStatus.BAD_REQUEST.value()
//
//        val result = getStatusFunction.withUploadId("1")
//        assertEquals(400, result.statusCode)
//    }
}

