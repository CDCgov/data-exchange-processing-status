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

class ApplicationKoinModulesTest {
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

    // Helper method for common koin app init logic
    private fun setupKoinApp(
        cloudProvider: String,
        modifyConfig: (ApplicationConfig) -> Unit = {}
    ): KoinApplication {
        // Mock ApplicationEnvironment and ApplicationConfig
        val mockEnvironment = mock(ApplicationEnvironment::class.java)
        val mockConfig = when (cloudProvider) {
            "aws" -> mockAwsConfig()
            "azure" -> mockAzureConfig()
            else -> mockAwsConfig() // Default to AWS config
        }

        // Apply any modifications to the config
        modifyConfig(mockConfig)

        // Set the mock config in the environment
        `when`(mockEnvironment.config).thenReturn(mockConfig)

        // Call the loadKoinModules function
        return koinApplication {
            loadKoinModules(mockEnvironment)
        }
    }

    @Test
    fun `test loadKoinModules with AWS config`() {
        val koinApp = setupKoinApp("aws")

        // Verify that CamelProcessor and EventProcessor have been loaded
        val camelProcessor = koinApp.koin.get<CamelProcessor>()
        val eventProcessor = koinApp.koin.get<EventProcessor>()

        // Ensure the processors are loaded
        assertEquals(CamelProcessor::class, camelProcessor::class)
        assertEquals(EventProcessor::class, eventProcessor::class)
    }

    @Test
    fun `test loadKoinModules with Azure config`() {
        val koinApp = setupKoinApp("azure")

        // Verify that CamelProcessor and EventProcessor have been loaded
        val camelProcessor = koinApp.koin.get<CamelProcessor>()
        val eventProcessor = koinApp.koin.get<EventProcessor>()

        // Ensure the processors are loaded
        assertEquals(CamelProcessor::class, camelProcessor::class)
        assertEquals(EventProcessor::class, eventProcessor::class)
    }

    @Test
    fun `test loadKoinModules unsupported cloud provider`() {
        val koinApp = setupKoinApp("UNKNOWN") {
            // Make the cloud.provider UNKNOWN
            `when`(it.property("cloud.provider")).thenReturn(
                MapApplicationConfig("cloud.provider" to "UNKNOWN").property("cloud.provider")
            )
        }

        // Check that the error message was logged
        val logMessages = logAppender.getLogMessages()
        assertTrue(logMessages.any { it.contains("Error loading Koin modules:") })
        assertTrue(logMessages.any { it.contains("Unsupported cloud provider: UNKNOWN") })
    }

    @Test
    fun `test loadKoinModules with AWS config missing a property`() {
        val koinApp = setupKoinApp("aws") {
            // Make the queue_name property return null
            `when`(it.property("cloud.aws.sqs.queue_name")).thenReturn(null)
        }

        // Check that the error message was logged
        val logMessages = logAppender.getLogMessages()
        assertTrue(logMessages.any { it.contains("Error loading Koin modules:") })
        assertTrue(logMessages.any { it.contains("Cannot invoke") })
    }

    @Test
    fun `test loadKoinModules with Azure config missing a property`() {
        val koinApp = setupKoinApp("azure") {
            // Make the connection_string property return null
            `when`(it.property("cloud.azure.service_bus.connection_string")).thenReturn(null)
        }

        // Check that the error message was logged
        val logMessages = logAppender.getLogMessages()
        assertTrue(logMessages.any { it.contains("Error loading Koin modules:") })
        assertTrue(logMessages.any { it.contains("Cannot invoke") })
    }

    // Helper method to mock aws config
    private fun mockAwsConfig(): ApplicationConfig {
        val mockConfig = mock(ApplicationConfig::class.java)

        `when`(mockConfig.property("cloud.provider")).thenReturn(MapApplicationConfig("cloud.provider" to "aws").property("cloud.provider"))
        `when`(mockConfig.property("cloud.aws.credentials.access_key_id")).thenReturn(
            MapApplicationConfig(
                "cloud.aws.credentials.access_key_id" to "mock-access-key",
            ).property("cloud.aws.credentials.access_key_id"),
        )
        `when`(mockConfig.property("cloud.aws.credentials.secret_access_key")).thenReturn(
            MapApplicationConfig(
                "cloud.aws.credentials.secret_access_key" to "mock-secret-key",
            ).property("cloud.aws.credentials.secret_access_key"),
        )
        `when`(
            mockConfig.property("cloud.aws.sqs.queue_name"),
        ).thenReturn(MapApplicationConfig("cloud.aws.sqs.queue_name" to "mock-queue-name").property("cloud.aws.sqs.queue_name"))
        `when`(
            mockConfig.property("cloud.aws.sqs.queue_url"),
        ).thenReturn(MapApplicationConfig("cloud.aws.sqs.queue_url" to "mock-queue-url").property("cloud.aws.sqs.queue_url"))
        `when`(
            mockConfig.property("cloud.aws.sqs.region"),
        ).thenReturn(MapApplicationConfig("cloud.aws.sqs.region" to "mock-region").property("cloud.aws.sqs.region"))
        `when`(
            mockConfig.property("cloud.aws.s3.bucket_name"),
        ).thenReturn(MapApplicationConfig("cloud.aws.s3.bucket_name" to "mock-bucket").property("cloud.aws.s3.bucket_name"))
        `when`(
            mockConfig.property("cloud.aws.s3.region"),
        ).thenReturn(MapApplicationConfig("cloud.aws.s3.region" to "mock-s3-region").property("cloud.aws.s3.region"))

        return mockConfig
    }

    // Helper method to mock azure config
    private fun mockAzureConfig(): ApplicationConfig {
        val mockConfig = mock(ApplicationConfig::class.java)

        `when`(mockConfig.property("cloud.provider")).thenReturn(MapApplicationConfig("cloud.provider" to "azure").property("cloud.provider"))
        `when`(mockConfig.property("cloud.azure.service_bus.namespace")).thenReturn(
            MapApplicationConfig(
                "cloud.azure.service_bus.namespace" to "mock-namespace",
            ).property("cloud.azure.service_bus.namespace"),
        )
        `when`(mockConfig.property("cloud.azure.service_bus.connection_string")).thenReturn(
            MapApplicationConfig(
                "cloud.azure.service_bus.connection_string" to "mock-connection-string",
            ).property("cloud.azure.service_bus.connection_string"),
        )
        `when`(mockConfig.property("cloud.azure.service_bus.shared_access_key_name")).thenReturn(
            MapApplicationConfig(
                "cloud.azure.service_bus.shared_access_key_name" to "mock-shared-access-key-name",
            ).property("cloud.azure.service_bus.shared_access_key_name"),
        )
        `when`(mockConfig.property("cloud.azure.service_bus.shared_access_key")).thenReturn(
            MapApplicationConfig(
                "cloud.azure.service_bus.shared_access_key" to "mock-shared-access-key",
            ).property("cloud.azure.service_bus.shared_access_key"),
        )
        `when`(mockConfig.property("cloud.azure.service_bus.topic_name")).thenReturn(
            MapApplicationConfig(
                "cloud.azure.service_bus.topic_name" to "mock-topic-name",
            ).property("cloud.azure.service_bus.topic_name"),
        )
        `when`(mockConfig.property("cloud.azure.service_bus.subscription_name")).thenReturn(
            MapApplicationConfig(
                "cloud.azure.service_bus.subscription_name" to "mock-subscription-name",
            ).property("cloud.azure.service_bus.subscription_name"),
        )
        `when`(mockConfig.property("cloud.azure.blob_storage.container_name")).thenReturn(
            MapApplicationConfig(
                "cloud.azure.blob_storage.container_name" to "mock-container-name",
            ).property("cloud.azure.blob_storage.container_name"),
        )
        `when`(mockConfig.property("cloud.azure.blob_storage.storage_account_key")).thenReturn(
            MapApplicationConfig(
                "cloud.azure.blob_storage.storage_account_key" to "mock-storage-account-key",
            ).property("cloud.azure.blob_storage.storage_account_key"),
        )
        `when`(mockConfig.property("cloud.azure.blob_storage.storage_account_name")).thenReturn(
            MapApplicationConfig(
                "cloud.azure.blob_storage.storage_account_name" to "mock-storage-account-name",
            ).property("cloud.azure.blob_storage.storage_account_name"),
        )
        `when`(mockConfig.property("cloud.azure.blob_storage.storage_endpoint_url")).thenReturn(
            MapApplicationConfig(
                "cloud.azure.blob_storage.storage_endpoint_url" to "mock-storage-endpoint-url",
            ).property("cloud.azure.blob_storage.storage_endpoint_url"),
        )

        return mockConfig
    }
}

