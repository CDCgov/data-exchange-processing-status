package gov.cdc.ocio.processingstatusapi

import gov.cdc.ocio.database.utils.DatabaseKoinCreator
import gov.cdc.ocio.processingstatusapi.plugins.configureRouting
import gov.cdc.ocio.processingstatusapi.plugins.graphQLModule
import gov.cdc.ocio.reportschemavalidator.utils.SchemaLoaderKoinCreator
import graphql.scalars.ExtendedScalars
import graphql.schema.idl.RuntimeWiring
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import org.koin.core.KoinApplication
import org.koin.ktor.plugin.Koin


fun KoinApplication.loadKoinModules(environment: ApplicationEnvironment): KoinApplication {
    val databaseModule = DatabaseKoinCreator.moduleFromAppEnv(environment)
    val schemaLoaderModule = SchemaLoaderKoinCreator.getSchemaLoaderFromAppEnv(environment)
    return modules(listOf(databaseModule, schemaLoaderModule))
}

fun main(args: Array<String>) {
    embeddedServer(Netty, commandLineEnvironment(args)).start(wait = true)

}

fun Application.module() {
    // Set the environment variable dynamically for Logback
    System.setProperty("ENVIRONMENT", environment.config.property("ktor.logback.environment").getString())

    install(Koin) {
        loadKoinModules(environment)
    }
    graphQLModule()
    configureRouting()

    install(ContentNegotiation) {
        jackson()
    }

    // See https://opensource.expediagroup.com/graphql-kotlin/docs/schema-generator/writing-schemas/scalars
    RuntimeWiring.newRuntimeWiring().scalar(ExtendedScalars.Date)
}
