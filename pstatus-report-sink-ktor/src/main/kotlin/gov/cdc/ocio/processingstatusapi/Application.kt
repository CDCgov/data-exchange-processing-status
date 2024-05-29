package gov.cdc.ocio.processingstatusapi

import gov.cdc.ocio.processingstatusapi.cosmos.CosmosDeadLetterRepository
import gov.cdc.ocio.processingstatusapi.cosmos.CosmosRepository
import gov.cdc.ocio.processingstatusapi.plugins.configureRouting
import gov.cdc.ocio.processingstatusapi.plugins.serviceBusModule
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.koin.core.KoinApplication
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.mp.KoinPlatform.getKoin

fun KoinApplication.loadKoinModules(environment: ApplicationEnvironment): KoinApplication {
    val cosmosModule = module {
        val uri = environment.config.property("azure.cosmos_db.client.endpoint").getString()
        val authKey = environment.config.property("azure.cosmos_db.client.key").getString()
        single { CosmosRepository(uri, authKey, "Reports", "/uploadId") }
        single { CosmosDeadLetterRepository(uri, authKey, "Reports-DeadLetter", "/uploadId") }
    }

    return modules(listOf(cosmosModule))
}

fun main(args: Array<String>) {
    embeddedServer(Netty, commandLineEnvironment(args)).start(wait = true)
}

fun Application.module() {
    configureRouting()
    serviceBusModule()
    install(Koin) {
        loadKoinModules(environment)
    }

    // Preload the koin module so the CosmosDB client is already initialized on the first call
    getKoin().get<CosmosRepository>()
}
