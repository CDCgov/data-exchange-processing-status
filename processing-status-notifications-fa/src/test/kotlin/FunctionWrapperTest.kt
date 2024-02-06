import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.ocio.functions.FunctionKotlinWrappers
import httpmock.HttpResponseMessageMock
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.testng.Assert
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import java.io.File
import java.util.*


class FunctionWrapperTest {

    private lateinit var request: HttpRequestMessage<Optional<String>>
    private lateinit var context: ExecutionContext
    private val testMessage = File("./src/test/kotlin/httpmock/response/subscribeEmail_badrequest.json").readText()

    @BeforeMethod
    fun setUp() {
        // Initialize mock objects or dependencies needed for testing
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
    fun testSubscribeEmail() {
        val response = FunctionKotlinWrappers().subscribeEmail(request, "destination-1", "eventType-1")
        assert(response.status == HttpStatus.OK)
    }

    @Test
    fun testSubscribeWebsocket() {
        val response = FunctionKotlinWrappers().subscribeEmail(request, "destination-1", "eventType-1")
        assert(response.status == HttpStatus.OK)
    }

    @Test
    fun testUnsubscribe() {
        var exceptionThrown = false
        try {
            FunctionKotlinWrappers().unsubscribe(request, "4dad617cd7de019066a0f49cad309948")
        } catch(ex: Exception) {
            exceptionThrown = true
        }
        Assert.assertTrue(exceptionThrown)
    }
}