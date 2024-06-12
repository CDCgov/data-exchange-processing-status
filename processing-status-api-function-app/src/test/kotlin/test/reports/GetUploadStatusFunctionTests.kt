package test.reports

import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.ocio.processingstatusapi.functions.status.GetUploadStatusFunction
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import utils.HttpResponseMessageMock
import java.util.*


class GetUploadStatusFunctionTests {

    private lateinit var request: HttpRequestMessage<Optional<String>>


    @BeforeMethod
    fun setUp() {
        request = mock(HttpRequestMessage::class.java) as HttpRequestMessage<Optional<String>>

        // Setup method invocation interception when createResponseBuilder is called to avoid null pointer on real method call.
        Mockito.doAnswer { invocation ->
            val status = invocation.arguments[0] as HttpStatus
            HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status)
        }.`when`(request).createResponseBuilder(any())
    }


    @Test
    fun testWithUploadStatusOk() {
        val response =  GetUploadStatusFunction(request).uploadStatus("1")
        assert(response.status == HttpStatus.OK)
    }

}

