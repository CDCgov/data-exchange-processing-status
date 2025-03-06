package gov.cdc.ocio.processingstatusapi.queries

import com.expediagroup.graphql.server.operations.Query
import gov.cdc.ocio.processingstatusapi.models.query.*
import gov.cdc.ocio.types.health.HealthStatusType
import gov.cdc.ocio.types.utils.TimeUtils
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.system.measureTimeMillis

/**
 * Service for querying the health of the report-sink service and its dependencies.
 *
 * @property logger KLogger

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
     * Returns a HealthCheck object with the overall health of the report-sink service and its dependencies.
     *
     * @return HealthCheck
     */
    suspend fun getHealth(): HealthResponse = coroutineScope {
        val serviceResults: MutableList<HealthCheck> = mutableListOf()
        var overallStatus = "UP"

        val time = measureTimeMillis {
            val healthChecks = configLoader.serviceConfigs.map { serviceConfig ->
                async {
                    if (serviceConfig.type == "internal") {
                        fetchGraphQLHealth(serviceConfig.name)
                    } else {
                        fetchExternalHealth(serviceConfig.name, serviceConfig.url!!)
                    }
                }
            }
            serviceResults.addAll(healthChecks.map { it.await() })
        }

        if (serviceResults.any { it.status.name == "DOWN" }) {
            overallStatus = HealthStatusType.STATUS_DOWN.toString()
        }

        return@coroutineScope HealthResponse(
            status = overallStatus,
            totalChecksDuration = TimeUtils.formatMillisToHMS(time),
            services = serviceResults
        )
    }

    /**
     * The function which fetches the graphql health
     * this function calls the graphQlHealth.getHealth directly
     * @param name string
     */
    private fun fetchGraphQLHealth(name: String): HealthCheck {
        val result = graphqlHealthService.getHealth()
        return HealthCheck(
            name = name,
            status = result.status!!,
            totalChecksDuration = result.totalChecksDuration!!,
            dependencyHealthChecks = result.dependencyHealthChecks.map {
                DependencyHealthCheck(it.system!!, it.service!!, it.status, it.healthIssues)
            }
        )
    }

    /**
     * Fetch external health Urls for services defined in the application.conf
     * @param name string
     * @param url string
     * @return ServiceHealth
     */
    private suspend fun fetchExternalHealth(name: String, url: String): HealthCheck {
        return try {
            val response: HealthCheck = client.get(url).body()
            response.copy(name = name)
        } catch (e: Exception) {
            logger.error(e) { "Failed to fetch health from $url" }
            HealthCheck(
                name = name,
                status = HealthStatusType.STATUS_DOWN,
                totalChecksDuration = "00:00:01.000",
                dependencyHealthChecks = listOf(
                    DependencyHealthCheck("Unknown", name, "DOWN", e.message)
                )
            )
        }
    }

}

/**
 * GraphQL query service for getting health status.
 */
class HealthQueryService : Query {
    suspend fun getHealth(): HealthResponse {
        return HealthCheckService().getHealth()
    }
}
