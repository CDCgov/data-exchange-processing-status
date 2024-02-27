package test.traces

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.ocio.processingstatusapi.cosmos.CosmosContainerManager
import gov.cdc.ocio.processingstatusapi.functions.traces.GetTraceFunction
import gov.cdc.ocio.processingstatusapi.functions.traces.TraceUtils
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


class GetTraceFunctionTests {

    private lateinit var context: ExecutionContext
    private lateinit var request: HttpRequestMessage<Optional<String>>
    private val testBytes = File("./src/test/kotlin/data/trace/get_trace.json").readText()
    private val mockResponse = mockk<Response>()
    // Convert the string to a JSONObject
    private val jsonObject = JSONObject(testBytes)

    @BeforeMethod
    fun setUp() {
        context = Mockito.mock(ExecutionContext::class.java)
        request = Mockito.mock(HttpRequestMessage::class.java) as HttpRequestMessage<Optional<String>>
        mockkStatic("khttp.KHttp")
        mockkObject(TraceUtils)
        every { TraceUtils.tracingEnabled } returns true
        every {khttp.get(any())} returns mockResponse
        every {mockResponse.jsonObject} returns jsonObject
        // Setup method invocation interception when createResponseBuilder is called to avoid null pointer on real method call.
        doAnswer { invocation ->
            val status = invocation.arguments[0] as HttpStatus
            HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status)
        }.`when`(request).createResponseBuilder(any())
    }

    @Test
    fun testWithTraceIdOk() {
        val getTraceFunction = GetTraceFunction(request)
        every {mockResponse.statusCode} returns HttpStatus.OK.value()

        val result = getTraceFunction.withTraceId("1")
        assertEquals(200, result.statusCode)
    }

    @Test
    fun testWithTraceIdBadRequest() {
        val getTraceFunction = GetTraceFunction(request)
        every {mockResponse.statusCode} returns HttpStatus.BAD_REQUEST.value()

        val result = getTraceFunction.withTraceId("1")
        assertEquals(400, result.statusCode)
    }

    @Test
    fun testWithUploadIdOk() {
        val getTraceFunction = GetTraceFunction(request)
        every {mockResponse.statusCode} returns HttpStatus.OK.value()

        val result = getTraceFunction.withUploadId("1")
        assertEquals(200, result.statusCode)
    }

    @Test
    fun testWithUploadIdBadRequest() {
        val getTraceFunction = GetTraceFunction(request)
        every {mockResponse.statusCode} returns HttpStatus.BAD_REQUEST.value()

        val result = getTraceFunction.withUploadId("1")
        assertEquals(400, result.statusCode)
    }
}

