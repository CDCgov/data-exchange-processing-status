package gov.cdc.ocio.rulesenginelib

import gov.cdc.ocio.rulesenginelib.gov.cdc.ocio.rulesenginelib.cosmos.CosmosDBService
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import org.koin.ktor.plugin.Koin


/**
 * Load the environment configuration values
 * Instantiate a singleton CosmosDatabase container instance
 * @param environment ApplicationEnvironment
 */
fun loadKoinModules(environment: ApplicationEnvironment) {

    /*val uri = environment.config.property("azure.cosmos_db.client.endpoint").getString()
    val authKey = environment.config.property("azure.cosmos_db.client.key").getString()
    val containerName = environment.config.property("azure.cosmos_db.client.containerName").getString()
    val partitionKey = environment.config.property("azure.cosmos_db.client.partitionKey").getString()

    val cosmosService = CosmosDBService(uri, authKey, containerName, partitionKey)
    val ruleEngineService = RuleEngineService(cosmosService)
    ruleEngineService.addEasyRule("upload from all jurisdictions should occur by 12pm")
    ruleEngineService.addWorkflowEmailRule(
        "condition2",
        listOf("email1@example.com", "email2@example.com")
    )*/
}

fun main(args: Array<String>) {
    embeddedServer(Netty, commandLineEnvironment(args)).start(wait = true)
}

/**
 * The main application module which always runs and loads other modules
 */
fun Application.module() {
    install(Koin) {
        loadKoinModules(environment)
    }
    install(ContentNegotiation) {
        jackson()
    }
}