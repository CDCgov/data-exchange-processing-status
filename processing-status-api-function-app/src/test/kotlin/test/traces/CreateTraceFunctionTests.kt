package test.traces

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.ocio.processingstatusapi.functions.traces.CreateTraceFunction
import gov.cdc.ocio.processingstatusapi.opentelemetry.OpenTelemetryConfig
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Tracer
import org.mockito.Mockito
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import utils.HttpResponseMessageMock
import java.io.File
import java.util.*


class CreateTraceFunctionTests {

    private lateinit var request: HttpRequestMessage<Optional<String>>
    private lateinit var context: ExecutionContext
    private val mockOpenTelemetry = mockk<OpenTelemetry>()
    private val mockTracer = mockk<Tracer>()
    private val testMessage = File("./src/test/kotlin/data/reports/createReport_badrequest.json").readText()

    @BeforeMethod
    fun setUp() {
        context = mock(ExecutionContext::class.java)
        request = mock(HttpRequestMessage::class.java) as HttpRequestMessage<Optional<String>>
        mockkObject(OpenTelemetryConfig)
        every { OpenTelemetryConfig.initOpenTelemetry()} returns mockOpenTelemetry
        every {mockOpenTelemetry.getTracer(CreateTraceFunction::class.java.name)} returns mockTracer

        // Setup method invocation interception when createResponseBuilder is called to avoid null pointer on real method call.
        doAnswer { invocation ->
            val status = invocation.arguments[0] as HttpStatus
            HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status)
        }.`when`(request).createResponseBuilder(any())
    }

    @Test
    fun testCreate_badrequest() {
        val response =  CreateTraceFunction(request).create();
        assert(response.status == HttpStatus.BAD_REQUEST)
    }

    @Test
    fun testCreate_uploadId_missing() {
        val queryParameters = mutableMapOf<String, String?>()
        queryParameters["destinationId"] = "1"
        queryParameters["eventType"] = "1"
        Mockito.`when`(request.body).thenReturn(Optional.of(testMessage))
        Mockito.`when`(request.queryParameters).thenReturn(queryParameters)
        val response =  CreateTraceFunction(request).create();
        assert(response.status == HttpStatus.BAD_REQUEST)
    }

    @Test
    fun testCreate_destinationId_missing() {
        val queryParameters = mutableMapOf<String, String?>()
        val testMessage = File("./src/test/kotlin/data/reports/createReport_badrequest.json").readText()
        queryParameters["eventType"] = "1"
        queryParameters["uploadId"] = "1"
        Mockito.`when`(request.body).thenReturn(Optional.of(testMessage))
        Mockito.`when`(request.queryParameters).thenReturn(queryParameters)
        val response =  CreateTraceFunction(request).create();
        assert(response.status == HttpStatus.BAD_REQUEST)
    }

    @Test
    fun testCreate_event_type_missing() {
        val queryParameters = mutableMapOf<String, String?>()
        val testMessage = File("./src/test/kotlin/data/reports/createReport_badrequest.json").readText()
        queryParameters["destinationId"] = "1"
        queryParameters["uploadId"] = "1"
        Mockito.`when`(request.body).thenReturn(Optional.of(testMessage))
        Mockito.`when`(request.queryParameters).thenReturn(queryParameters)
        val response =  CreateTraceFunction(request).create();
        assert(response.status == HttpStatus.BAD_REQUEST)
    }

    //@Test
    fun testCreate_uploadId_good_request() {
        val queryParameters = mutableMapOf<String, String?>()
        val testMessage = File("./src/test/kotlin/data/reports/createReport_badrequest.json").readText()
        queryParameters["uploadId"] = "1"
        queryParameters["destinationId"] = "1"
        queryParameters["eventType"] = "1"
        Mockito.`when`(request.body).thenReturn(Optional.of(testMessage))
        Mockito.`when`(request.queryParameters).thenReturn(queryParameters)
        val response =  CreateTraceFunction(request).create();
        assert(response.status == HttpStatus.BAD_REQUEST)
    }

}

