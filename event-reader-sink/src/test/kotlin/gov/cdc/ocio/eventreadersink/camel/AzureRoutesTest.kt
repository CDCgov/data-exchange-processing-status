package gov.cdc.ocio.eventreadersink.camel

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.azure.storage.blob.BlobClient
import com.azure.storage.blob.models.BlobStorageException
import gov.cdc.ocio.eventreadersink.util.TestLogAppender
import io.mockk.every
import io.mockk.mockk
import org.apache.camel.builder.AdviceWith
import org.apache.camel.builder.AdviceWithRouteBuilder
import org.apache.camel.component.mock.MockEndpoint
import org.apache.camel.test.junit5.CamelTestSupport
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

class AzureRoutesTest : CamelTestSupport() {
    private lateinit var azureRoutes: AzureRoutes

    // Setup logging capture
    private val logger: Logger = LoggerFactory.getLogger("org.apache.camel.processor.errorhandler.DefaultErrorHandler") as Logger
    private val logAppender = TestLogAppender()

    @BeforeEach
    fun setup() {
        logger.level = Level.DEBUG
        logger.addAppender(logAppender)
        logAppender.start()
    }

    @AfterEach
    fun cleanup() {
        logAppender.clear()
        context.stop() // Stop the Camel context if necessary
    }

    @Test
    fun `test Azure route initialization and message processing`() {
        // Simulate Service Bus queue as a direct endpoint for message input to the route
        val mockServiceBusEndpoint = "direct://serviceBus"
        // Mock blob container endpoint for assertions on messages
        val mockBlobEndpoint = "mock:azure-storage-blob://testAccount/testContainer"

        val mockBlobClient = mockk<BlobClient>()
        every { mockBlobClient.upload(any(), any(), true) } returns Unit // Mock the upload action

        // Set fixed timestamp
        val fixedTimeProvider = { 1729691041456L }

        // Initialize AzureRoutes with valid configuration
        azureRoutes =
            AzureRoutes(
                topicName = "testTopic",
                subscriptionName = "testSubscription",
                accountName = "testAccount",
                accountKey = "testKey",
                containerName = "testContainer",
                storageEndpointURL = "",
                timeProvider = fixedTimeProvider,
                customFromUri = mockServiceBusEndpoint,
            )

        context.addRoutes(azureRoutes)

        val routeDefinition = context.getRouteDefinition("azureBlobRoute")
        AdviceWith.adviceWith(
            routeDefinition,
            context,
            object : AdviceWithRouteBuilder() {
                override fun configure() {
                    replaceFromWith(mockServiceBusEndpoint) // Redirect the `from` endpoint to mock
                    weaveByToUri("azure-storage-blob://*").replace().to(mockBlobEndpoint) // Redirect blob storage to mock endpoint
                }
            },
        )

        // Start Camel context after configuring routes
        context.start()

        // Verify route started
        assertTrue(context.routeDefinitions.isNotEmpty())

        // Mock the blob storage endpoint
        val mockBlob = context.getEndpoint(mockBlobEndpoint, MockEndpoint::class.java)

        // Prepare mock expectations
        mockBlob.expectedMessageCount(1)
        mockBlob.expectedHeaderReceived("CamelAzureStorageBlobBlobName", "message-1729691041456.json")

        // Simulate sending a JSON message to mock SQS queue
        val jsonPayload = """{"name": "Test User", "email": "test@example.com"}"""
        template.sendBody(mockServiceBusEndpoint, jsonPayload)

        // Verify message reached the blob storage endpoint
        mockBlob.assertIsSatisfied()
    }

    @Test
    fun `test Azure route error handling with retries`() {
        // Simulate Service Bus queue as a direct endpoint for message input to the route
        val mockServiceBusEndpoint = "direct://serviceBus"
        // Mock blob container endpoint for assertions on messages
        val mockBlobEndpoint = "mock:azure-storage-blob://testAccount/testContainer"

        val mockBlobClient = mockk<BlobClient>()
        every { mockBlobClient.upload(any(), any(), true) } returns Unit // Mock the upload action

        // Set fixed timestamp
        val fixedTimeProvider = { 1729691041456L }

        // Initialize AzureRoutes with valid configuration
        azureRoutes =
            AzureRoutes(
                topicName = "testTopic",
                subscriptionName = "testSubscription",
                accountName = "testAccount",
                accountKey = "testKey",
                containerName = "testContainer",
                storageEndpointURL = "",
                timeProvider = fixedTimeProvider,
                customFromUri = mockServiceBusEndpoint,
            )

        context.addRoutes(azureRoutes)

        val routeDefinition = context.getRouteDefinition("azureBlobRoute")
        AdviceWith.adviceWith(
            routeDefinition,
            context,
            object : AdviceWithRouteBuilder() {
                override fun configure() {
                    replaceFromWith(mockServiceBusEndpoint) // Redirect the `from` endpoint to mock
                    weaveByToUri("azure-storage-blob://*").replace().to(mockBlobEndpoint) // Redirect blob storage to mock endpoint
                }
            },
        )

        // Start Camel context after configuring routes
        context.start()

        // Verify route started
        assertTrue(context.routeDefinitions.isNotEmpty())

        // Mock the blob storage endpoint
        val mockBlob = context.getEndpoint(mockBlobEndpoint, MockEndpoint::class.java)
        // Simulate an exception in the route
        mockBlob.whenAnyExchangeReceived { throw BlobStorageException("Simulated Exception", null, null) }

        // Prepare mock expectations (1 attempted & 3 retries)
        mockBlob.expectedMessageCount(4)
//        mockBlob.expectedHeaderReceived("CamelAzureStorageBlobBlobName", "message-1729691041456.json")

        // Simulate sending a JSON message to mock SQS queue
        val jsonPayload = """{"name": "Test User", "email": "test@example.com"}"""
        template.sendBody(mockServiceBusEndpoint, jsonPayload)

        // Verify message reached the blob storage endpoint
        mockBlob.assertIsSatisfied()

        // Check logs...
        val logMessages = logAppender.getLogMessages()

        // Define the expected delivery messages
        val expectedMessages =
            listOf(
                "Failed delivery for .* On delivery attempt: 0 caught: com.azure.storage.blob.models.BlobStorageException: Simulated Exception",
                "Failed delivery for .* On delivery attempt: 1 caught: com.azure.storage.blob.models.BlobStorageException: Simulated Exception",
                "Failed delivery for .* On delivery attempt: 2 caught: com.azure.storage.blob.models.BlobStorageException: Simulated Exception",
                "Failed delivery for .* On delivery attempt: 3 caught: com.azure.storage.blob.models.BlobStorageException: Simulated Exception",
            )

        // Assert that each expected messages appears
        expectedMessages.forEachIndexed { index, expectedMessage ->
            val matchingLogs = logMessages.filter { it.matches(Regex(expectedMessage)) }
            assertTrue(matchingLogs.isNotEmpty(), "Expected log for delivery attempt $index not found.")
        }
    }
}
