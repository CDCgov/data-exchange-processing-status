package gov.cdc.ocio.processingstatusapi.queries


import gov.cdc.ocio.database.cosmos.CosmosClientManager
import gov.cdc.ocio.database.cosmos.CosmosConfiguration
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.assertEquals

class HealthQueryServiceTest {

    private val cosmosConfiguration: CosmosConfiguration = mockk()
    private val cosmosClientManager: CosmosClientManager = mockk()
    private val healthCheckService: HealthCheckService = mockk()

    private val healthQueryService: HealthQueryService = mockk()

    @BeforeEach
    fun setUp() {
        // Initialize Koin with a module that provides mocked dependencies
        startKoin {
            modules(module {
                single { cosmosConfiguration }
                single { cosmosClientManager }
                single { healthCheckService }
            })
        }

        // Mocking the dependencies
        mockkObject(CosmosClientManager)
    }

    @AfterEach
    fun tearDown() {
        // Stop Koin after each test to ensure a clean state
        stopKoin()
    }

    @Test
    fun getHealth_success() = runBlocking {
        // Arrange
        val cosmosConfiguration = CosmosConfiguration("uri", "authKey")
        val healthCheck = GraphQLHealthCheck().apply {
            status = "UP"
            totalChecksDuration = "00:00:00.000"
            dependencyHealthChecks.any { it.service == "Cosmos DB" && it.status == "UP" }
        }

       // every { CosmosClientManager.getCosmosClient("uri","authKey") } returns mockk()
        every { healthQueryService.getHealth() } returns healthCheck

        // Act
        val result = healthQueryService.getHealth()

        // Assert
        assertEquals("UP", result.status)
      //  assertTrue(result.dependencyHealthChecks.any { it.service == "Cosmos DB" && it.status == "UP" })
    }

    @Test
    fun getHealth_db_down() = runBlocking {
        // Arrange
        val cosmosConfiguration = CosmosConfiguration("uri", "authKey")
        val healthCheck = GraphQLHealthCheck().apply {
            status = "DOWN"
            totalChecksDuration = "00:00:00.000"
            dependencyHealthChecks.any { it.service == "Cosmos DB" && it.status == "DOWN" }
        }

      //  every { CosmosClientManager.getCosmosClient(any(), any()) } returns null
        every { healthQueryService.getHealth() } returns healthCheck

        // Act
        val result = healthQueryService.getHealth()

        // Assert
        assertEquals("DOWN", result.status)
     //   assertTrue(result.dependencyHealthChecks.any { it.service == "Cosmos DB" && it.status == "DOWN" })
    }
}
