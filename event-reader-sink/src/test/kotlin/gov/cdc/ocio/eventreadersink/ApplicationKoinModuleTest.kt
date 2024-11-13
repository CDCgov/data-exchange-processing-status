package gov.cdc.ocio.eventreadersink

import ch.qos.logback.classic.Logger
import gov.cdc.ocio.eventreadersink.sink.CamelProcessor
import gov.cdc.ocio.eventreadersink.sink.EventProcessor
import gov.cdc.ocio.eventreadersink.util.TestLogAppender
import io.ktor.server.application.ApplicationEnvironment
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.config.MapApplicationConfig
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.KoinApplication
import org.koin.dsl.koinApplication
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.slf4j.LoggerFactory
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApplicationKoinModuleTest {
    // Setup logging capture
    private val logger: Logger = LoggerFactory.getLogger("gov.cdc.ocio.eventreadersink.Application") as Logger
    private val logAppender = TestLogAppender()

    @BeforeEach
    fun setup() {
        logger.addAppender(logAppender)
        logAppender.start()
    }

    @AfterEach
    fun tearDown() {
        // Clear log messages after each test
        logAppender.clear()
    }

    @Test
    fun `test loadKoinModules with AWS config`() {
        val koinApp = setupKoinApp("aws")
        assertModulesLoaded(koinApp)
    }

    @Test
    fun `test loadKoinModules with Azure config`() {
        val koinApp = setupKoinApp("azure")
        assertModulesLoaded(koinApp)
    }

    @Test
    fun `test loadKoinModules unsupported cloud provider`() {
        val koinApp =
            setupKoinApp("UNKNOWN")

        // Check that the error message was logged
        val logMessages = logAppender.getLogMessages()
        assertTrue(logMessages.any { it.contains("Error loading Koin modules:") })
        assertTrue(logMessages.any { it.contains("Unsupported cloud provider: UNKNOWN") })
    }

    @Test
    fun `test loadKoinModules with AWS config missing a property`() {
        val koinApp =
            setupKoinApp("aws") { properties ->
                properties.remove("cloud.aws.sqs.queue_name")
            }

        // Check that the error message was logged
        val logMessages = logAppender.getLogMessages()
        assertTrue(logMessages.any { it.contains("Error loading Koin modules:") })
        assertTrue(logMessages.any { it.contains("Cannot invoke") })
    }

    @Test
    fun `test loadKoinModules with Azure config missing a property`() {
        val koinApp =
            setupKoinApp("azure") { properties ->
                properties.remove("cloud.azure.service_bus.connection_string")
            }

        // Check that the error message was logged
        val logMessages = logAppender.getLogMessages()
        assertTrue(logMessages.any { it.contains("Error loading Koin modules:") })
        assertTrue(logMessages.any { it.contains("Cannot invoke") })
    }

    // Helper method for common Koin app initialization logic
    private fun setupKoinApp(
        cloudProvider: String,
        modifyProperties: (MutableMap<String, String>) -> Unit = {},
    ): KoinApplication {
        val properties = getMockProperties(cloudProvider).toMutableMap()
        modifyProperties(properties)
        val mockConfig = createMockConfig(properties)
        val mockEnvironment = mock(ApplicationEnvironment::class.java)
        `when`(mockEnvironment.config).thenReturn(mockConfig)
        return koinApplication {
            loadKoinModules(mockEnvironment)
        }
    }

    // Helper method to assert modules are loaded
    private fun assertModulesLoaded(koinApp: KoinApplication) {
        // Verify that CamelProcessor and EventProcessor have been loaded
        val camelProcessor = koinApp.koin.get<CamelProcessor>()
        val eventProcessor = koinApp.koin.get<EventProcessor>()

        // Ensure the processors are loaded
        assertEquals(CamelProcessor::class, camelProcessor::class)
        assertEquals(EventProcessor::class, eventProcessor::class)
    }

    // Helper method to create a mock config with given properties
    private fun createMockConfig(properties: Map<String, String>): ApplicationConfig {
        val mockConfig = mock(ApplicationConfig::class.java)
        properties.forEach { (key, value) ->
            `when`(mockConfig.property(key)).thenReturn(MapApplicationConfig(key to value).property(key))
        }
        return mockConfig
    }

    // Helper method to get mock properties based on cloud provider
    private fun getMockProperties(cloudProvider: String): Map<String, String> =
        when (cloudProvider.lowercase()) {
            "aws" ->
                mapOf(
                    "cloud.provider" to "aws",
                    "cloud.aws.credentials.access_key_id" to "mock-access-key",
                    "cloud.aws.credentials.secret_access_key" to "mock-secret-key",
                    "cloud.aws.sqs.queue_name" to "mock-queue-name",
                    "cloud.aws.sqs.queue_url" to "mock-queue-url",
                    "cloud.aws.sqs.region" to "mock-region",
                    "cloud.aws.s3.bucket_name" to "mock-bucket",
                    "cloud.aws.s3.region" to "mock-s3-region",
                )
            "azure" ->
                mapOf(
                    "cloud.provider" to "azure",
                    "cloud.azure.service_bus.namespace" to "mock-namespace",
                    "cloud.azure.service_bus.connection_string" to "mock-connection-string",
                    "cloud.azure.service_bus.shared_access_key_name" to "mock-shared-access-key-name",
                    "cloud.azure.service_bus.shared_access_key" to "mock-shared-access-key",
                    "cloud.azure.service_bus.topic_name" to "mock-topic-name",
                    "cloud.azure.service_bus.subscription_name" to "mock-subscription-name",
                    "cloud.azure.blob_storage.container_name" to "mock-container-name",
                    "cloud.azure.blob_storage.storage_account_key" to "mock-storage-account-key",
                    "cloud.azure.blob_storage.storage_account_name" to "mock-storage-account-name",
                    "cloud.azure.blob_storage.storage_endpoint_url" to "mock-storage-endpoint-url",
                )
            else ->
                mapOf(
                    "cloud.provider" to cloudProvider,
                )
        }
}
