package gov.cdc.ocio.pstatus.topic.sink.blob


import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import org.koin.core.KoinApplication
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import gov.cdc.ocio.pstatus.topic.sink.blob.camel.*

/**
 * Load the environment configuration values
 * Instantiate a singleton CosmosDatabase container instance
 * @param environment ApplicationEnvironment
 */

fun KoinApplication.loadKoinModules(environment: ApplicationEnvironment) {

    val namespace = environment.config.property("azure.service_bus.namespace").getString()
    val connectionString = environment.config.property("azure.service_bus.connection_string").getString()
    val sharedAccessKeyName = environment.config.property("azure.service_bus.shared_access_key_name").getString()
    val sharedAccessKey = environment.config.property("azure.service_bus.shared_access_key").getString()
    val topicName = environment.config.property("azure.service_bus.topic_name").getString()
    val subscriptionName = environment.config.property("azure.service_bus.subscription_name").getString()
    val containerName = environment.config.property("azure.blob_storage.container_name").getString()
    val storageAccountKey = environment.config.property("azure.blob_storage.storage_account_key").getString()
    val storageAccountName = environment.config.property("azure.blob_storage.storage_account_name").getString()

    CamelProcessor().sinkAsbTopicSubscriptionToBlob(connectionString,storageAccountName,storageAccountKey,containerName,
                                                     namespace,sharedAccessKeyName,sharedAccessKey, topicName, subscriptionName)
}


/**
 * The main function
 *  @param args Array<string>
 */

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

