package gov.cdc.ocio.processingstatusapi.queries

import gov.cdc.ocio.processingstatusapi.models.query.HealthStatusResult
import gov.cdc.ocio.types.health.HealthCheck
import gov.cdc.ocio.types.health.HealthCheckResult
import gov.cdc.ocio.types.health.HealthStatusType
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals


class HealthQueryServiceTest {

    private val healthCheckService: HealthCheckService = mockk()

    @Test
    fun `getHealth should return UP status`() = runBlocking {
        // Arrange
        val serviceHealth = HealthCheck().apply {
            this.name = "Couchbase DB"
            this.status = HealthStatusType.STATUS_UP
            this.totalChecksDuration = "00:00:00.123"
            this.dependencyHealthChecks = listOf(
                HealthCheckResult("Database", "Couchbase", HealthStatusType.STATUS_UP, null)).toMutableList()
        }
        val healthResponse = HealthStatusResult(
            status = "UP",
            totalChecksDuration = "00:00:00.123",
            services = listOf(serviceHealth)
        )

        coEvery { healthCheckService.getHealth() } returns healthResponse

        // Act
        val healthQueryService = HealthQueryService(healthCheckService)
        val result = healthQueryService.getHealth()

        // Assert
        assertEquals("UP", result.status)
        assertEquals("00:00:00.123", result.totalChecksDuration)
        assertEquals(1, result.services.size)
        assertEquals("Couchbase DB", result.services.first().name)
        assertEquals(HealthStatusType.STATUS_UP, result.services.first().status)
    }

    @Test
    fun `getHealth should return DOWN status when a service is unavailable`() = runBlocking {
        // Arrange
        val serviceHealth = HealthCheck().apply {
            this.name = "Couchbase DB"
            this.status = HealthStatusType.STATUS_DOWN
            this.totalChecksDuration = "00:00:00.456"
            this.dependencyHealthChecks = listOf(
                HealthCheckResult("Database", "Couchbase DB", HealthStatusType.STATUS_DOWN, "Timeout")).toMutableList()
        }

        val healthResponse = HealthStatusResult(
            status = "DOWN",
            totalChecksDuration = "00:00:00.456",
            services = listOf(serviceHealth)
        )

        coEvery { healthCheckService.getHealth() } returns healthResponse

        // Act
        val healthQueryService = HealthQueryService(healthCheckService)
        val result = healthQueryService.getHealth()

        // Assert
        assertEquals("DOWN", result.status)
        assertEquals("00:00:00.456", result.totalChecksDuration)
        assertEquals(1, result.services.size)
        assertEquals("Couchbase DB", result.services.first().name)
        assertEquals(HealthStatusType.STATUS_DOWN, result.services.first().status)
        assertEquals("Timeout", result.services.first().dependencyHealthChecks.first().healthIssues)
    }
}
