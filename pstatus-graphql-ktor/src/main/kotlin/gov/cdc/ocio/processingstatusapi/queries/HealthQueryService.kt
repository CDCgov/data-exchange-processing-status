package gov.cdc.ocio.processingstatusapi.queries

import com.expediagroup.graphql.server.operations.Query
import gov.cdc.ocio.processingstatusapi.models.query.*
import gov.cdc.ocio.types.health.HealthCheck
import gov.cdc.ocio.types.health.HealthCheckResult
import gov.cdc.ocio.types.health.HealthStatusType
import gov.cdc.ocio.types.utils.TimeUtils
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.system.measureTimeMillis


/**
 * Service responsible for performing health checks on the application and its dependencies.
 * Utilizes both internal and external health checks to determine the overall health status of the system.
 */
class HealthCheckService: KoinComponent {

    private val logger = KotlinLogging.logger {}
    private val configLoader by inject<HealthConfigLoader>()
    private val graphqlHealthService by inject<GraphQLHealth>()
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true }) // Allows unknown JSON fields
        }
    }

    /**
     * Retrieves the overall health status of multiple services along with detailed health information for each service.
     *
     * Aggregates and evaluates the health status of configured services, differentiating between internal
     * and external services. Internal services are checked using GraphQL health checks, while external services
     * are checked via respective URLs. Also calculates the total duration for all health checks.
     *
     * @return HealthStatusResult containing the overall health status, total duration of health checks,
     *         and a list of detailed health check results for individual services.
     */
    suspend fun getHealth(): HealthStatusResult = coroutineScope {
        // Filter services: include internal or external with non-null URL
        val services = configLoader.serviceConfigs.filter {
            it.type == "internal" || it.url != null
        }

        val serviceResults: List<HealthCheck>
        val time = measureTimeMillis {
            val healthChecks = services.map { serviceConfig ->
                async {
                    if (serviceConfig.type == "internal") {
                        fetchGraphQLHealth(serviceConfig.name)
                    } else {
                        // Only append "/health" if URL is defined
                        fetchExternalHealth(serviceConfig.name, serviceConfig.url + "/health")
                    }
                }
            }
            serviceResults = healthChecks.awaitAll()
        }

        val overallStatus = if (serviceResults.any { it.status == HealthStatusType.STATUS_DOWN }) {
            HealthStatusType.STATUS_DOWN.value
        } else {
            "UP"
        }

        HealthStatusResult(
            status = overallStatus,
            totalChecksDuration = TimeUtils.formatMillisToHMS(time),
            services = serviceResults
        )
    }

    /**
     * Fetches the GraphQL health status for a service and returns a `HealthCheck` object with its details.
     *
     * @param name The name of the service for which the health status is being fetched.
     * @return A `HealthCheck` object containing the health status, total checks duration, and dependency health check details.
     */
    private fun fetchGraphQLHealth(name: String): HealthCheck {
        val result = graphqlHealthService.getHealth()
        return HealthCheck().apply {
            this.name = name
            this.status = result.status!!
            this.totalChecksDuration = result.totalChecksDuration!!
            this.dependencyHealthChecks = result.dependencyHealthChecks.map {
                HealthCheckResult(
                    it.system!!, it.service!!,
                    if (it.status == "UP") HealthStatusType.STATUS_UP else HealthStatusType.STATUS_DOWN, it.healthIssues
                )
            }.toMutableList()
        }
    }

    /**
     * Fetches external health information from a specified URL and returns a `HealthCheck` object.
     * If an error occurs during the process, a default `HealthCheck` object with error details is returned.
     *
     * @param name The name of the service for which health is being fetched.
     * @param url The URL to fetch the health information from.
     * @return A `HealthCheck` object containing the health status and additional details.
     */
    private suspend fun fetchExternalHealth(name: String, url: String): HealthCheck {
        return try {
            val response: HealthCheck = client.get(url).body()
           response.apply { this.name=name }
        } catch (e: Exception) {
            logger.error(e) { "Failed to fetch health from $url" }
            HealthCheck().apply{
                this.name = name
                this.status = HealthStatusType.STATUS_DOWN
                this.totalChecksDuration = "00:00:01.000"
                this.dependencyHealthChecks = listOf(
                    HealthCheckResult("Unknown", name, HealthStatusType.STATUS_DOWN, e.message)
                ).toMutableList()
            }
        }
    }
}

/**
 * Service responsible for querying health status information.
 *
 * The `HealthQueryService` acts as a GraphQL query implementation for retrieving
 * the health status of application services. It leverages `HealthCheckService`
 * to perform the underlying health check evaluations and provides results in the form of
 * `HealthStatusResult`. This is intended to expose the overall health status and detailed
 * health results of the application's components through GraphQL queries.
 *
 * @constructor Instantiates the service with a `HealthCheckService` used to perform health checks.
 */
class HealthQueryService(
    private val healthCheckService: HealthCheckService = HealthCheckService()
) : Query {
    suspend fun getHealth(): HealthStatusResult {
        return healthCheckService.getHealth()
    }
}
