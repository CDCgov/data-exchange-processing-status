package test

import com.azure.cosmos.CosmosClient
import com.azure.cosmos.CosmosContainer
import com.azure.cosmos.CosmosDatabase
import com.azure.cosmos.util.CosmosPagedIterable
import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.ocio.processingstatusapi.cosmos.CosmosContainerManager
import gov.cdc.ocio.processingstatusapi.functions.HealthCheckFunction
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


class HealthCheckFunctionTest {

    private lateinit var request: HttpRequestMessage<Optional<String>>
    private lateinit var context: ExecutionContext

    @BeforeMethod
    fun setUp() {
        // Initialize any mock objects or dependencies needed for testing
        request = mock(HttpRequestMessage::class.java) as HttpRequestMessage<Optional<String>>
        context = mock(ExecutionContext::class.java)

        // Setup method invocation interception when createResponseBuilder is called to avoid null pointer on real method call.
        Mockito.doAnswer { invocation ->
            val status = invocation.arguments[0] as HttpStatus
            HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status)
        }.`when`(request).createResponseBuilder(any())
    }

    @Test
    fun testStatusBack() {
        mockkObject(CosmosContainerManager)
        val mockCosmosClient = mockk<CosmosClient>()
        val mockCosmosDb = mockk<CosmosDatabase>()
        val mockCosmosContainer = mockk<CosmosContainer>()

        val items = mockk<CosmosPagedIterable<Report>>()

        every { mockCosmosClient.getDatabase(any()) } returns mockCosmosDb
        every { mockCosmosDb.getContainer(any()) } returns mockCosmosContainer
        every { mockCosmosContainer.queryItems(any<String>(), any(), Report::class.java) } returns items

        // Create a HealthCheckFunction instance
        val healthCheckFunction = HealthCheckFunction(request)

        // call HealthCheckFunction
        val response = healthCheckFunction.run()

        assert(response.status == HttpStatus.INTERNAL_SERVER_ERROR)
    }

     @Test
    fun testFailureStatusBack() {   
        val mockCosmosClient = mockk<CosmosClient>()
        every { mockCosmosClient.getDatabase(any()) } throws (Exception("CosmosDB error"))
        // Create a HealthCheckFunction instance
        val healthCheckFunction = HealthCheckFunction(request)
        // call HealthCheckFunction
        val response = healthCheckFunction.run()
    }
}