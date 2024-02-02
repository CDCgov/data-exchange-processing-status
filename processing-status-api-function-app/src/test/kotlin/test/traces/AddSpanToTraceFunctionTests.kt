package test.traces

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.ocio.processingstatusapi.functions.traces.AddSpanToTraceFunction
import gov.cdc.ocio.processingstatusapi.opentelemetry.OpenTelemetryConfig
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Tracer
import khttp.responses.Response
import org.mockito.Mockito
import org.mockito.Mockito.doAnswer
import org.mockito.kotlin.any
import org.testng.Assert.assertEquals
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import utils.HttpResponseMessageMock
import java.io.File
import java.util.*


class AddSpanToTraceFunctionTests {

    private lateinit var context: ExecutionContext
    private lateinit var request: HttpRequestMessage<Optional<String>>
    val testMessage = File("./src/test/kotlin/data/reports/createReport_badrequest.json").readText()
    private val mockResponse = mockk<Response>()
    private val mockOpenTelemetry = mockk<OpenTelemetry>()
    private val mockTracer = mockk<Tracer>()

    @BeforeMethod
    fun setUp() {
        context = Mockito.mock(ExecutionContext::class.java)
        request = Mockito.mock(HttpRequestMessage::class.java) as HttpRequestMessage<Optional<String>>
        mockkObject(OpenTelemetryConfig)
        every { OpenTelemetryConfig.initOpenTelemetry()} returns mockOpenTelemetry
        every {mockOpenTelemetry.getTracer(AddSpanToTraceFunction::class.java.name)} returns mockTracer
        Mockito.`when`(request.body).thenReturn(Optional.of(testMessage))
        // Setup method invocation interception when createResponseBuilder is called to avoid null pointer on real method call.
        doAnswer { invocation ->
            val status = invocation.arguments[0] as HttpStatus
            HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status)
        }.`when`(request).createResponseBuilder(any())
    }

    @Test
    fun testWithStartSpanOk() {
        val addSpanToTraceFunction = AddSpanToTraceFunction(request)
        every {mockResponse.statusCode} returns HttpStatus.OK.value()

        val result = addSpanToTraceFunction.startSpan("1", "1")
        assertEquals(400, result.statusCode)
    }

    //@Test
    fun testWithStopSpan_ok() {
        val addSpanToTraceFunction = AddSpanToTraceFunction(request)
        every {mockResponse.statusCode} returns HttpStatus.OK.value()

        val result = addSpanToTraceFunction.stopSpan("1", "1")
        assertEquals(200, result.statusCode)
    }




}

