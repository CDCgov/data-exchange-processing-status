import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.ocio.processingstatusapi.FunctionJavaWrappers
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
    private val testMessage = File("./src/test/kotlin/data/reports/createReport_badrequest.json").readText()

    @BeforeMethod
    fun setUp() {
        // Initialize any mock objects or dependencies needed for testing
        request = mock(HttpRequestMessage::class.java) as HttpRequestMessage<Optional<String>>
        context = mock(ExecutionContext::class.java)
        Mockito.`when`(request.body).thenReturn(Optional.of(testMessage))

        // Setup method invocation interception when createResponseBuilder is called to avoid null pointer on real method call.
        Mockito.doAnswer { invocation ->
            val status = invocation.arguments[0] as HttpStatus
            HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status)
        }.`when`(request).createResponseBuilder(org.mockito.kotlin.any())
    }

    @Test
    fun testHealthCheck() {
        val response = FunctionJavaWrappers().healthCheck(request)
        assert(response.status == HttpStatus.OK)
    }

    @Test
    fun testCreateTrace() {
        val response = FunctionJavaWrappers().createTrace(request)
    }

    @Test
    fun testTraceStartSpan() {
        var exceptionThrown = false
        try {
            FunctionJavaWrappers().traceStartSpan(request, "", "")
        } catch(ex: Exception) {
            exceptionThrown = true
        }
        Assert.assertTrue(exceptionThrown)
    }

    @Test
    fun testTraceStopSpan() {
        var exceptionThrown = false
        try {
            FunctionJavaWrappers().traceStopSpan(request, "", "")
        } catch(ex: Exception) {
            exceptionThrown = true
        }
        Assert.assertTrue(exceptionThrown)
    }

    @Test
    fun testGetTraceByTraceId() {
        var exceptionThrown = false
        try {
            FunctionJavaWrappers().getTraceByTraceId(request, "")
        } catch(ex: Exception) {
            exceptionThrown = true
        }
        Assert.assertTrue(exceptionThrown)
    }

    @Test
    fun testGetTraceByUploadId() {
        var exceptionThrown = false
        try {
            FunctionJavaWrappers().getTraceByUploadId(request, "")
        } catch(ex: Exception) {
            exceptionThrown = true
        }
        Assert.assertTrue(exceptionThrown)
    }

    @Test
    fun testGetTraceSpanByUploadIdStageName() {
        val response = FunctionJavaWrappers().getTraceSpanByUploadIdStageName(request)
    }

    @Test
    fun tesCreateReportByUploadId() {
        val response = FunctionJavaWrappers().createReportByUploadId(request, "")
    }

    @Test
    fun testReplaceReportByUploadId() {
        val response = FunctionJavaWrappers().replaceReportByUploadId(request, "")
    }

    @Test
    fun testGetReportByUploadId() {
        var exceptionThrown = false
        try {
            FunctionJavaWrappers().getReportByUploadId(request, "")
        } catch(ex: Exception) {
            exceptionThrown = true
        }
        Assert.assertTrue(exceptionThrown)
    }

    @Test
    fun testGetReportByReportId() {
        var exceptionThrown = false
        try {
            FunctionJavaWrappers().getReportByReportId(request, "")
        } catch(ex: Exception) {
            exceptionThrown = true
        }
        Assert.assertTrue(exceptionThrown)
    }

    @Test
    fun testGetUploadStatus() {
        val response = FunctionJavaWrappers().getUploadStatus(request, "")
        assert(response.status == HttpStatus.OK)
    }

    @Test
    fun testGetReportByStage() {
        var exceptionThrown = false
        try {
            FunctionJavaWrappers().getReportByStage(request, "", "", context)
        } catch(ex: Exception) {
            exceptionThrown = true
        }
        Assert.assertTrue(exceptionThrown)
    }
    @Test
    fun testGetStatusByUploadId() {
        var exceptionThrown = false
        try {
            FunctionJavaWrappers().getStatusByUploadId(request, "", context)
        } catch(ex: Exception) {
            exceptionThrown = true
        }
        Assert.assertTrue(exceptionThrown)
    }
}