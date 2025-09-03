package gov.cdc.ocio.processingstatusapi.models.query


import io.ktor.server.config.*

class HealthConfigLoader (config: ApplicationConfig) {
    data class ServiceConfig(val name: String, val url: String?, val type: String)

    val serviceConfigs: List<ServiceConfig> = config.configList("health.services").map {
        ServiceConfig(
            name = it.property("name").getString(),
            url = it.propertyOrNull("url")?.getString(),
            type = it.property("type").getString()
        )
    }
    fun getInternalServiceName(): String? = serviceConfigs.firstOrNull { it.type == "internal" }?.name
}