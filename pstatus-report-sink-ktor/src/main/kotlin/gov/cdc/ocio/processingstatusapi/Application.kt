package gov.cdc.ocio.processingstatusapi

import gov.cdc.ocio.database.telemetry.Otel
import gov.cdc.ocio.database.utils.DatabaseKoinCreator
import gov.cdc.ocio.messagesystem.models.MessageSystemType
import gov.cdc.ocio.messagesystem.utils.MessageSystemKoinCreator
import gov.cdc.ocio.messagesystem.utils.createMessageSystemPlugin
import gov.cdc.ocio.messagesystem.utils.MessageProcessorConfigKoinCreator
import gov.cdc.ocio.processingstatusapi.processors.AWSSQSProcessor
import gov.cdc.ocio.processingstatusapi.processors.RabbitMQProcessor
import gov.cdc.ocio.processingstatusapi.processors.ServiceBusProcessor
import gov.cdc.ocio.processingstatusapi.processors.UnsupportedProcessor
import gov.cdc.ocio.reportschemavalidator.utils.SchemaLoaderKoinCreator
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.instrumentation.ktor.v2_0.KtorServerTelemetry
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk
import io.opentelemetry.sdk.metrics.Aggregation
import io.opentelemetry.sdk.metrics.InstrumentSelector
import io.opentelemetry.sdk.metrics.InstrumentType
import io.opentelemetry.sdk.metrics.View
import io.opentelemetry.semconv.ServiceAttributes
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
    val messageSystemModule = MessageSystemKoinCreator.moduleFromAppEnv(environment)
    val messageProcessorConfigModule = MessageProcessorConfigKoinCreator.moduleFromAppEnv(environment)

    return modules(
        listOf(
            databaseModule,
            schemaLoaderModule,
            messageSystemModule,
            messageProcessorConfigModule
        )
    )
}

/**
 * The main function
 * @param args Array<string>
 */
fun main(args: Array<String>) {
    embeddedServer(Netty, commandLineEnvironment(args)).start(wait = true)
}

/**
 * The main application module which always runs and loads other modules
 */
fun Application.module() {
    // Set the environment variable dynamically for Logback
    System.setProperty("ENVIRONMENT", environment.config.property("ktor.logback.environment").getString())

    configureRouting()

    val messageSystemType = MessageSystemType.getFromAppEnv(environment)

    val messageProcessor = when (messageSystemType) {
        MessageSystemType.AWS -> AWSSQSProcessor()
        MessageSystemType.AZURE_SERVICE_BUS -> ServiceBusProcessor()
        MessageSystemType.RABBITMQ -> RabbitMQProcessor()
        else -> UnsupportedProcessor()
    }

    createMessageSystemPlugin(messageSystemType, messageProcessor)

    val builder = AutoConfiguredOpenTelemetrySdk.builder()
        .setResultAsGlobal()
        .addResourceCustomizer { old, _ ->
        old.toBuilder()
            .putAll(old.attributes)
            .put(ServiceAttributes.SERVICE_NAME, environment.config.tryGetString("otel.service_name") ?: "pstatus-report-sink")
            .build()
        }
        .addMeterProviderCustomizer { old, _ ->
            old.registerView(
                InstrumentSelector.builder().setType(InstrumentType.HISTOGRAM).build(), Otel.getDefaultHistogramView())
        }
    val otel: OpenTelemetry = builder.build().openTelemetrySdk
    install(KtorServerTelemetry) {
        setOpenTelemetry(otel)
    }

    install(Koin) {
        loadKoinModules(environment)
    }
    install(ContentNegotiation) {
        jackson()
    }
}
