package functions

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpStatus
import functions.httpMockData.HttpResponseMessageMock
import gov.cdc.ocio.FunctionJavaWrappers
import gov.cdc.ocio.functions.servicebus.ReportsNotificationsSBQueueProcessor
import org.mockito.Mockito
import org.testng.Assert.*
import org.testng.annotations.BeforeTest
import org.testng.annotations.Test
import java.io.File
import java.util.*

/**
 * This class tests subscribing for certain notifications and validating
 * if the respective subscribed notifications were sent out
 *
 * @property context ExecutionContext
 * @property request HttpRequestMessage<Optional<String>>
 * @property testMessage String
 * @property queryParameters Map<String, String>
 */
class RulesEngineTest {

    private lateinit var context: ExecutionContext
    private lateinit var request: HttpRequestMessage<Optional<String>>
    private val testMessage = File("./src/test/kotlin/functions/httpMockData/subscribeEmail_badrequest.json").readText()

    @BeforeTest
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

    @Test(description = "Test for email notification for valid destinationId/eventType")
    fun testHL7ReportWithDestination1EventTypeSuccess() {

        val queryParameters: Map<String, String>  =  mapOf("email" to "uei@hdk.com",
            "stageName" to "dex-hl7-validation",
            "statusType" to "success")

        Mockito.`when`(request.queryParameters).thenReturn(queryParameters)
        val response2 = FunctionJavaWrappers().subscribeEmail(request, "dex-testing", "test-event2")

        val testMessage = File("./src/test/kotlin/functions/rulesEngineMockData/sb_good_message_hl7_report1_with_eventType2.json").readText()
        val status = ReportsNotificationsSBQueueProcessor(context).withTestMessageForDispatch(testMessage)
        assertEquals(status, listOf("Email Event dispatched for uei@hdk.com",""))
    }

    @Test(description = "Test for email notification for invalid destinationId/eventType")
    fun testHL7Report2WithDestination1EventType2Success() {
        val queryParameters: Map<String, String>  =  mapOf("email" to "uei@hdk.com",
            "stageName" to "dex-hl7-validation",
            "statusType" to "success")
        Mockito.`when`(request.queryParameters).thenReturn(queryParameters)
        val response1 = FunctionJavaWrappers().subscribeEmail(request, "dex-testing", "test-event1")

        val testMessage = File("./src/test/kotlin/functions/rulesEngineMockData/sb_good_message_hl7_report2_with_stageName1.json").readText()
        val status = ReportsNotificationsSBQueueProcessor(context).withTestMessageForDispatch(testMessage)
        assertEquals(status, listOf("",""))
    }

    @Test(description = "Test for email notification for invalid status type and valid destinationId/eventType/stageName")
    fun testReportValidArgsInValidStatusTypeFailure() {
        val queryParameters: Map<String, String>  =  mapOf("stageName" to "dex-hl7-validation-1",
            "statusType" to "warning",
            "url" to "ws://192.34.46/24/45")
        Mockito.`when`(request.queryParameters).thenReturn(queryParameters)
        val response = FunctionJavaWrappers().subscribeWebsocket(request, "dex-testing", "test-event1")

        val testMessage = File("./src/test/kotlin/functions/rulesEngineMockData/sb_good_message_hl7_report2_with_stageName1.json").readText()
        val status = ReportsNotificationsSBQueueProcessor(context).withTestMessageForDispatch(testMessage)
        assertEquals(status, listOf("",""))
    }

    @Test(description = "Test for email notification for valid status type of failure and valid destinationId/eventType/stageName")
    fun testReportValidArgsValidStatusTypeFailure() {
        val queryParameters: Map<String, String>  =  mapOf("stageName" to "dex-hl7-validation-1",
            "statusType" to "success",
            "url" to "ws://192.34.46/24/45")
        Mockito.`when`(request.queryParameters).thenReturn(queryParameters)
        val response = FunctionJavaWrappers().subscribeWebsocket(request, "dex-testing", "test-event1")

        val testMessage = File("./src/test/kotlin/functions/rulesEngineMockData/sb_good_message_hl7_report2_with_stageName1.json").readText()
        val status = ReportsNotificationsSBQueueProcessor(context).withTestMessageForDispatch(testMessage)
        assertEquals(status, listOf("","Websocket Event dispatched for ws://192.34.46/24/45"))
    }

    @Test(description = "Test for email and websocket notification for valid statusType and destinationId/eventType/stageName")
    fun testHL7Report2WithDestination1EventType1Success() {
        val queryParameters: Map<String, String>  =  mapOf("email" to "uei@hdk.com",
            "stageName" to "dex-hl7-validation",
            "statusType" to "success",
            "url" to "ws://192.34.46/24/45")
        Mockito.`when`(request.queryParameters).thenReturn(queryParameters)
        val response1 = FunctionJavaWrappers().subscribeEmail(request, "dex-testing", "test-event1")
        val response2 = FunctionJavaWrappers().subscribeWebsocket(request, "dex-testing", "test-event1")

        val testMessage = File("./src/test/kotlin/functions/rulesEngineMockData/sb_good_message_hl7_report1_with_eventType1.json").readText()
        val status = ReportsNotificationsSBQueueProcessor(context).withTestMessageForDispatch(testMessage)
        assertEquals(status, listOf("Email Event dispatched for uei@hdk.com","Websocket Event dispatched for ws://192.34.46/24/45"))
    }
}