import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.ocio.processingstatusapi.FunctionJavaWrappers
import gov.cdc.ocio.processingstatusapi.functions.traces.CreateTraceFunction
import gov.cdc.ocio.processingstatusapi.opentelemetry.OpenTelemetryConfig
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Tracer
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.testng.Assert
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import utils.HttpResponseMessageMock
import java.io.File
import java.util.*


class FunctionWrapperTest {

    private lateinit var request: HttpRequestMessage<Optional<String>>
    private lateinit var context: ExecutionContext
    private val mockOpenTelemetry = mockk<OpenTelemetry>()
    private val mockTracer = mockk<Tracer>()
    private val testMessage = File("./src/test/kotlin/data/reports/createReport_badrequest.json").readText()

    @BeforeMethod
    fun setUp() {
        // Initialize any mock objects or dependencies needed for testing
        request = mock(HttpRequestMessage::class.java) as HttpRequestMessage<Optional<String>>
        context = mock(ExecutionContext::class.java)
        Mockito.`when`(request.body).thenReturn(Optional.of(testMessage))
        mockkObject(OpenTelemetryConfig)
        every { OpenTelemetryConfig.initOpenTelemetry()} returns mockOpenTelemetry
        every {mockOpenTelemetry.getTracer(CreateTraceFunction::class.java.name)} returns mockTracer

        // Setup method invocation interception when createResponseBuilder is called to avoid null pointer on real method call.
        Mockito.doAnswer { invocation ->
            val status = invocation.arguments[0] as HttpStatus
            HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status)
        }.`when`(request).createResponseBuilder(org.mockito.kotlin.any())
    }

    @Test
    fun testHealthCheck() {
        val response = FunctionJavaWrappers().healthCheck(request)
        assert(response.status == HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @Test
    fun testCreateTrace() {
        val response = FunctionJavaWrappers().createTrace(request)
    }

    @Test
    fun testTraceStartSpan() {
        var exceptionThrown = false
        try {
            FunctionJavaWrappers().traceStartSpan(request, "4dad617cd7de019066a0f49cad309948", "1")
        } catch(ex: Exception) {
            exceptionThrown = true
        }
        Assert.assertTrue(exceptionThrown)
    }

    @Test
    fun testTraceStopSpan() {
        var exceptionThrown = false
        try {
            FunctionJavaWrappers().traceStopSpan(request, "4dad617cd7de019066a0f49cad309948", "1")
        } catch(ex: Exception) {
            exceptionThrown = true
        }
        Assert.assertTrue(exceptionThrown)
    }

    @Test
    fun testGetTraceByTraceId() {
        var exceptionThrown = false
        try {
            FunctionJavaWrappers().getTraceByTraceId(request, "4dad617cd7de019066a0f49cad309948")
        } catch(ex: Exception) {
            exceptionThrown = true
        }
        Assert.assertFalse(exceptionThrown)
    }

    @Test
    fun testGetTraceByUploadId() {
        var exceptionThrown = false
        try {
            FunctionJavaWrappers().getTraceByUploadId(request, "4dad617cd7de019066a0f49cad309948")
        } catch(ex: Exception) {
            exceptionThrown = true
        }
        Assert.assertFalse(exceptionThrown)
    }

    @Test
    fun testGetTraceSpanByUploadIdStageName() {
        val response = FunctionJavaWrappers().getTraceSpanByUploadIdStageName(request)
    }

    @Test
    fun tesCreateReportByUploadId() {
        val response = FunctionJavaWrappers().createReportByUploadId(request, "4dad617cd7de019066a0f49cad309948")
    }

    @Test
    fun testReplaceReportByUploadId() {
        val response = FunctionJavaWrappers().replaceReportByUploadId(request, "4dad617cd7de019066a0f49cad309948")
    }

    @Test
    fun testGetReportByUploadId() {
        var exceptionThrown = false
        try {
            FunctionJavaWrappers().getReportByUploadId(request, "4dad617cd7de019066a0f49cad309948")
        } catch(ex: Exception) {
            exceptionThrown = true
        }
        Assert.assertTrue(exceptionThrown)
    }

    @Test
    fun testGetReportByReportId() {
        var exceptionThrown = false
        try {
            FunctionJavaWrappers().getReportByReportId(request, "4dad617cd7de019066a0f49cad309948")
        } catch(ex: Exception) {
            exceptionThrown = true
        }
        Assert.assertTrue(exceptionThrown)
    }

    @Test
    fun testGetUploadStatus() {
        val response = FunctionJavaWrappers().getUploadStatus(request, "4dad617cd7de019066a0f49cad309948")
        assert(response.status == HttpStatus.OK)
    }

    @Test
    fun testGetReportByStage() {
        var exceptionThrown = false
        try {
            FunctionJavaWrappers().getReportByStage(request, "4dad617cd7de019066a0f49cad309948", "START", context)
        } catch(ex: Exception) {
            exceptionThrown = true
        }
        Assert.assertTrue(exceptionThrown)
    }
    @Test
    fun testGetStatusByUploadId() {
        var exceptionThrown = false
        try {
            FunctionJavaWrappers().getStatusByUploadId(request, "4dad617cd7de019066a0f49cad309948", context)
        } catch(ex: Exception) {
            exceptionThrown = true
        }
        Assert.assertTrue(exceptionThrown)
    }
}