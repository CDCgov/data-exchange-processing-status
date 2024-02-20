package functions

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpStatus
import functions.httpMockData.HttpResponseMessageMock
import gov.cdc.ocio.FunctionJavaWrappers
import gov.cdc.ocio.functions.servicebus.ReportsNotificationsSBQueueProcessor
import org.mockito.Mockito
import org.testng.Assert.*
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import java.io.File
import java.util.*

class RuleEngineTest {

    private lateinit var context: ExecutionContext
    private lateinit var request: HttpRequestMessage<Optional<String>>
    private val testMessage = File("./src/test/kotlin/httpmock/response/subscribeEmail_badrequest.json").readText()
    private val queryParameters: Map<String, String>  =  mapOf("email" to "uei@hdk.com",
        "stageName" to "dex-hl7-validation",
        "statusType" to "success",
        "url" to "ws://192.34.46/24/45")

    @BeforeMethod
    fun setUp() {
        context = Mockito.mock(ExecutionContext::class.java)
        // Initialize mock objects or dependencies needed for testing
        request = Mockito.mock(HttpRequestMessage::class.java) as HttpRequestMessage<Optional<String>>
        Mockito.`when`(request.body).thenReturn(Optional.of(testMessage))

        // Setup method invocation interception when createResponseBuilder is called to avoid null pointer on real method call.
        Mockito.doAnswer { invocation ->
            val status = invocation.arguments[0] as HttpStatus
            HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status)
        }.`when`(request).createResponseBuilder(org.mockito.kotlin.any())
    }

    @Test(priority=0)
    fun testSubscribeEmailSuccess() {
        Mockito.`when`(request.queryParameters).thenReturn(queryParameters)
        val response = FunctionJavaWrappers().subscribeEmail(request, "destination-1", "eventType-1")
        assert(response.status == HttpStatus.OK)
    }

    // HL7 Report Tests
    @Test(priority=1, description = "Test for valid json content format in hL7 report with all 'SUCCESS' status")
    fun testValidHL7ReportWithSuccess() {
        val testMessage = File("./src/test/kotlin/functions/serviceMockData/hl7/sb_good_message_hl7_report_with_success.json").readText()
        val status = ReportsNotificationsSBQueueProcessor(context).withMessage(testMessage)
        assertEquals(status, "success")
    }




}