package test.reports

import com.azure.cosmos.CosmosClient
import com.azure.cosmos.CosmosContainer
import com.azure.cosmos.CosmosDatabase
import com.azure.cosmos.util.CosmosPagedIterable
import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.ocio.processingstatusapi.cosmos.CosmosContainerManager
import gov.cdc.ocio.processingstatusapi.functions.reports.GetReportFunction
import gov.cdc.ocio.processingstatusapi.model.reports.Report
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import utils.HttpResponseMessageMock
import java.util.*


class GetReportFunctionTests {

    private lateinit var request: HttpRequestMessage<Optional<String>>

    private lateinit var context: ExecutionContext
    private val items = mockk<CosmosPagedIterable<Report>>()
    private val mockCosmosClient = mockk<CosmosClient>()
    private val mockCosmosDb = mockk<CosmosDatabase>()
    private val mockCosmosContainer = mockk<CosmosContainer>()

    @BeforeMethod
    fun setUp() {
        request = mock(HttpRequestMessage::class.java) as HttpRequestMessage<Optional<String>>
        context = mock(ExecutionContext::class.java)
        mockkObject(CosmosContainerManager)

        every { CosmosContainerManager.initDatabaseContainer(any(), any())} returns mockCosmosContainer
        every { mockCosmosClient.getDatabase(any()) } returns mockCosmosDb
        every { mockCosmosDb.getContainer(any()) } returns mockCosmosContainer
        every { mockCosmosContainer.queryItems(any<String>(), any(), Report::class.java) } returns items


        // Setup method invocation interception when createResponseBuilder is called to avoid null pointer on real method call.
        Mockito.doAnswer { invocation ->
            val status = invocation.arguments[0] as HttpStatus
            HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status)
        }.`when`(request).createResponseBuilder(any())
    }


    @Test
    fun testWithUploadId_ok() {
        every { items.count() > 0} returns false
        val response =  GetReportFunction(request).withUploadId("1");
        assert(response.status == HttpStatus.BAD_REQUEST)
    }

    @Test
    fun testWithReportId_ok() {
        every { items.count() > 0} returns false
        val response =  GetReportFunction(request).withReportId("1");
        assert(response.status == HttpStatus.BAD_REQUEST)
    }

    @Test
    fun testWithDestinationId_ok() {
        every { items.count() > 0} returns false
        val response =  GetReportFunction(request).withDestinationId("1", "");
        assert(response.status == HttpStatus.OK)
    }

    //@Test
    fun testWithUploadId_reports() {
        var itemsR =
        every { mockCosmosContainer.queryItems(any<String>(), any(), Report::class.java) } returns items
        every { items.count() > 0} returns true
        val response =  GetReportFunction(request).withUploadId("1");
        assert(response.status == HttpStatus.OK)
    }

}

