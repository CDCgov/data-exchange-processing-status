package gov.cdc.ocio.processingstatusapi

import gov.cdc.ocio.processingstatusapi.cosmos.CosmosRepository
import gov.cdc.ocio.processingstatusapi.plugins.graphQLModule
import gov.cdc.ocio.processingstatusapi.plugins.serviceBusModule
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.koin.dsl.module
import org.koin.ktor.plugin.koin
import org.koin.mp.KoinPlatform.getKoin

val koinModule = module {
    single { CosmosRepository("Reports", "/uploadId") }
}

fun main(args: Array<String>) {
    embeddedServer(Netty, commandLineEnvironment(args)).start(wait = true)
}

fun Application.module() {
//    configureRouting()
    graphQLModule()
    serviceBusModule()
    koin {
        modules(koinModule)
    }

    // Preload the koin module so the CosmosDB client is already initialized on the first call
    getKoin().get<CosmosRepository>()
}
