package gov.cdc.ocio.eventreadersink.camel

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import gov.cdc.ocio.eventreadersink.model.AwsConfig
import gov.cdc.ocio.eventreadersink.util.TestLogAppender
import io.mockk.every
import io.mockk.mockk
import org.apache.camel.component.mock.MockEndpoint
import org.apache.camel.test.junit5.CamelTestSupport
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

class AwsRoutesTest : CamelTestSupport() {
    private lateinit var awsRoutes: AwsRoutes

    // Mock AwsConfig
    private val mockAwsConfig = mockk<AwsConfig>(relaxed = true)

    // Setup logging capture
    private val logger: Logger = LoggerFactory.getLogger("org.apache.camel.processor.errorhandler.DefaultErrorHandler") as Logger
    private val logAppender = TestLogAppender()

    // Common mock vars
    private val mockSqsEndpoint = "direct:sqs"
    private val mockS3Endpoint = "mock:aws2-s3://testBucket"
    private val fixedTimeProvider = { 1729691041456L }
    private val jsonPayload = """{"name": "Test User", "email": "test@example.com"}"""

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

    // Helper method for common mock aws route init logic
    private fun setupAwsRoute(exceptionSimulation: Boolean = false): MockEndpoint {
        // Setup the AwsRoutes configuration
        configureMockAwsConfig(mockAwsConfig)

        awsRoutes =
            AwsRoutes(
                awsConfig = mockAwsConfig,
                timeProvider = fixedTimeProvider,
                sqsEndpoint = mockSqsEndpoint,
                s3Endpoint = mockS3Endpoint,
            )

        context.addRoutes(awsRoutes)

        // Start the context
        context.start()

        // Verify route started
        assertTrue(context.routeDefinitions.size > 0)

        // Mock the S3 endpoint
        val mockS3 = context.getEndpoint(mockS3Endpoint, MockEndpoint::class.java)

        if (exceptionSimulation) {
            // Simulate an exception in the route
            mockS3.whenAnyExchangeReceived { throw RuntimeException("Simulated Exception") }
        }

        return mockS3
    }

    @Test
    fun `test AWS route initialization and message processing`() {
        val mockS3 = setupAwsRoute()

        // Prepare mock expectations
        mockS3.expectedMessageCount(1)
        mockS3.expectedHeaderReceived("CamelAwsS3Key", "message-1729691041456.json")

        // Simulate sending a JSON message to mock SQS queue
        template.sendBody(mockSqsEndpoint, jsonPayload)

        // Verify message arrived at mock S3 endpoint
        mockS3.assertIsSatisfied()
    }

    @Test
    fun `test AWS route error handling with retries`() {
        val mockS3 = setupAwsRoute(exceptionSimulation = true)

        // Prepare mock expectations (1 attempted & 3 retries)
        mockS3.expectedMessageCount(4)

        // Simulate sending a JSON message to mock SQS queue
        template.sendBody(mockSqsEndpoint, jsonPayload)

        mockS3.assertIsSatisfied()

        // Check logs...
        val logMessages = logAppender.getLogMessages()

        // Define the expected delivery messages
        val expectedMessages =
            listOf(
                "Failed delivery for .* On delivery attempt: 0 caught: java.lang.RuntimeException: Simulated Exception",
                "Failed delivery for .* On delivery attempt: 1 caught: java.lang.RuntimeException: Simulated Exception",
                "Failed delivery for .* On delivery attempt: 2 caught: java.lang.RuntimeException: Simulated Exception",
                "Failed delivery for .* On delivery attempt: 3 caught: java.lang.RuntimeException: Simulated Exception",
            )

        // Assert that each expected message appears
        expectedMessages.forEachIndexed { index, expectedMessage ->
            val matchingLogs = logMessages.filter { it.matches(Regex(expectedMessage)) }
            assertTrue(matchingLogs.isNotEmpty(), "Expected log for delivery attempt $index not found.")
        }
    }

    // Helper method for mocking AwsConfig
    private fun configureMockAwsConfig(mockAwsConfig: AwsConfig) {
        every { mockAwsConfig.accessKeyId } returns "testAccessKey"
        every { mockAwsConfig.secretAccessKey } returns "testSecretKey"
        every { mockAwsConfig.sqsQueueName } returns "testQueue"
        every { mockAwsConfig.sqsQueueURL } returns "testQueueURL"
        every { mockAwsConfig.sqsRegion } returns "us-east-1"
        every { mockAwsConfig.s3EndpointURL } returns "testEndpointURL"
        every { mockAwsConfig.s3BucketName } returns "testBucket"
        every { mockAwsConfig.s3Region } returns "us-east-1"
    }
}
