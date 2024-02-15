package functions

import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import functions.httpMockData.HttpResponseMessageMock
import gov.cdc.ocio.FunctionJavaWrappers
import gov.cdc.ocio.exceptions.BadStateException
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.testng.Assert
import org.testng.Assert.*
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import java.io.File
import java.util.*

class FunctionWrapperTest {

    private lateinit var request: HttpRequestMessage<Optional<String>>
    private val testMessage = File("./src/test/kotlin/httpmock/response/subscribeEmail_badrequest.json").readText()
    private val queryParameters: Map<String, String>  =  mapOf("email" to "abc@def.ghi",
        "stageName" to "dummyStage",
        "statusType" to "warning",
        "url" to "ws://192.34.46/24/45")


    @BeforeMethod
    fun setUp() {
        // Initialize mock objects or dependencies needed for testing
        request = mock(HttpRequestMessage::class.java) as HttpRequestMessage<Optional<String>>
        Mockito.`when`(request.body).thenReturn(Optional.of(testMessage))

                // Setup method invocation interception when createResponseBuilder is called to avoid null pointer on real method call.
        Mockito.doAnswer { invocation ->
            val status = invocation.arguments[0] as HttpStatus
            HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status)
        }.`when`(request).createResponseBuilder(org.mockito.kotlin.any())
    }

    @Test
    fun testSubscribeEmailSuccess() {
        Mockito.`when`(request.queryParameters).thenReturn(queryParameters)
        val response = FunctionJavaWrappers().subscribeEmail(request, "destination-1", "eventType-1")
        assert(response.status == HttpStatus.OK)
    }

    @Test
    fun testSubscribeEmailMissingQueryParameters() {
        val response = FunctionJavaWrappers().subscribeEmail(request, "destination-1", "eventType-1")
        assert(response.status == HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @Test
    fun testSubscribeWebsocketMissingUrl() {
        val response = FunctionJavaWrappers().subscribeWebsocket(request, "destination-1", "eventType-1")
        assert(response.status == HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @Test
    fun testSubscribeWebsocketSuccess() {
        Mockito.`when`(request.queryParameters).thenReturn(queryParameters)
        val response = FunctionJavaWrappers().subscribeWebsocket(request, "destination-1", "eventType-1")
        assert(response.status == HttpStatus.OK)
    }

    @Test
    fun testUnsubscribeError() {
        var response: HttpResponseMessage? = null
        try {
            response = FunctionJavaWrappers().unsubscribe(request, "")
        } catch(e: BadStateException) {
            assertTrue(response?.status  == HttpStatus.INTERNAL_SERVER_ERROR)
        }


    }
}