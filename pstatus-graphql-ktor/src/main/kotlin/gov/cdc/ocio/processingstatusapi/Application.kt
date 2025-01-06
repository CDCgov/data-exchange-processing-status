package gov.cdc.ocio.processingstatusapi

import gov.cdc.ocio.database.utils.DatabaseKoinCreator
import gov.cdc.ocio.processingstatusapi.plugins.configureRouting
import gov.cdc.ocio.processingstatusapi.plugins.graphQLModule
import gov.cdc.ocio.reportschemavalidator.utils.SchemaLoaderConfigurationKoinCreator
import graphql.scalars.ExtendedScalars
import graphql.schema.idl.RuntimeWiring
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import org.koin.core.KoinApplication
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin


fun KoinApplication.loadKoinModules(environment: ApplicationEnvironment): KoinApplication {
    val appEnvironmentModule = module { single { environment } }
    val databaseModule = DatabaseKoinCreator.moduleFromAppEnv(environment)
    val healthCheckDatabaseModule = DatabaseKoinCreator.dbHealthCheckModuleFromAppEnv(environment)
    val schemaConfigurationModule = SchemaLoaderConfigurationKoinCreator.getSchemaLoaderConfigurationFromAppEnv(environment)
    return modules(listOf(appEnvironmentModule, databaseModule, healthCheckDatabaseModule,schemaConfigurationModule))
}

fun main(args: Array<String>) {
    embeddedServer(Netty, commandLineEnvironment(args)).start(wait = true)
}

fun Application.module() {

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
