package gov.cdc.ocio.processingstatusapi

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import gov.cdc.ocio.database.utils.DatabaseKoinCreator
import gov.cdc.ocio.messagesystem.utils.MessageProcessorConfigKoinCreator
import gov.cdc.ocio.messagesystem.utils.MessageSystemKoinCreator
import gov.cdc.ocio.processingstatusapi.plugins.configureRouting
import gov.cdc.ocio.processingstatusapi.plugins.graphQLModule
import gov.cdc.ocio.reportschemavalidator.utils.SchemaLoaderKoinCreator
import gov.cdc.ocio.processingstatusapi.utils.SchemaSecurityConfigKoinCreator
import graphql.scalars.ExtendedScalars
import graphql.schema.idl.RuntimeWiring
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import org.koin.core.KoinApplication
import org.koin.ktor.plugin.Koin


/**
 * Load the environment configuration values
 *
 * @receiver KoinApplication
 * @param environment ApplicationEnvironment
 * @return KoinApplication
 */
fun KoinApplication.loadKoinModules(environment: ApplicationEnvironment): KoinApplication {
    val databaseModule = DatabaseKoinCreator.moduleFromAppEnv(environment)
    val schemaLoaderModule = SchemaLoaderKoinCreator.moduleFromAppEnv(environment)
    val schemaSecurityConfig = SchemaSecurityConfigKoinCreator.moduleFromAppEnv(environment)
    val messageSystemModule = MessageSystemKoinCreator.moduleFromAppEnv(environment)
    val messageProcessorConfigModule = MessageProcessorConfigKoinCreator.moduleFromAppEnv(environment)

    return modules(
        listOf(
            databaseModule,
            schemaLoaderModule,
            schemaSecurityConfig,
            messageSystemModule,
            messageProcessorConfigModule
        )
    )
}

fun main(args: Array<String>) {
    EngineMain.main(args)
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
        jackson {
            registerModule(JavaTimeModule()) // Support java.time.*
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }
    }

    // See https://opensource.expediagroup.com/graphql-kotlin/docs/schema-generator/writing-schemas/scalars
    RuntimeWiring.newRuntimeWiring().scalar(ExtendedScalars.Date)
}
