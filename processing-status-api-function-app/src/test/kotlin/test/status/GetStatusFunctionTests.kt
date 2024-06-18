package test.status

import com.azure.cosmos.CosmosClient
import com.azure.cosmos.CosmosContainer
import com.azure.cosmos.CosmosDatabase
import com.azure.cosmos.util.CosmosPagedIterable
import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.ocio.processingstatusapi.cosmos.CosmosContainerManager
import gov.cdc.ocio.processingstatusapi.functions.status.GetStatusFunction
import gov.cdc.ocio.processingstatusapi.model.reports.Report
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import khttp.responses.Response
import org.json.JSONObject
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import utils.HttpResponseMessageMock
import java.io.File
import java.util.*


class GetStatusFunctionTests {

    private lateinit var request: HttpRequestMessage<Optional<String>>
    private lateinit var context: ExecutionContext

    private val testBytes = File("./src/test/kotlin/data/trace/get_trace.json").readText()
    private val mockResponse = mockk<Response>()
    // Convert the string to a JSONObject
    private val jsonObject = JSONObject(testBytes)
    private val items = mockk<CosmosPagedIterable<Report>>()

    @BeforeMethod
    fun setUp() {
        context = mock(ExecutionContext::class.java)
        mockkObject(CosmosContainerManager)
        val mockCosmosClient = mockk<CosmosClient>()
        val mockCosmosDb = mockk<CosmosDatabase>()
        val mockCosmosContainer = mockk<CosmosContainer>()

        every { CosmosContainerManager.initDatabaseContainer(any(), any()) } returns mockCosmosContainer
        every { mockCosmosClient.getDatabase(any()) } returns mockCosmosDb
        every { mockCosmosDb.getContainer(any()) } returns mockCosmosContainer
        request = mock(HttpRequestMessage::class.java) as HttpRequestMessage<Optional<String>>
        mockkStatic("khttp.KHttp")
        every {khttp.get(any())} returns mockResponse
        every {mockResponse.jsonObject} returns jsonObject
        every { mockCosmosContainer.queryItems(any<String>(), any(), Report::class.java) } returns items

        // Setup method invocation interception when createResponseBuilder is called to avoid null pointer on real method call.
        doAnswer { invocation ->
            val status = invocation.arguments[0] as HttpStatus
            HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status)
        }.`when`(request).createResponseBuilder(any())
    }

    @Test
    fun testCreateSuccess() {
        every { items.count() > 0} returns false

        every {mockResponse.statusCode} returns HttpStatus.OK.value()
        val response = GetStatusFunction(request).withUploadId("1");
        assert(response.status == HttpStatus.OK)
    }


}

