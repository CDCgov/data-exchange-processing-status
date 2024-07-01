package gov.cdc.ocio.pstatus.asbtopics.blob


import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.koin.core.KoinApplication
import org.koin.ktor.plugin.Koin
import gov.cdc.ocio.pstatus.asbtopics.blob.camel.CamelProcessor
/**
 * Load the environment configuration values
 * Instantiate a singleton CosmosDatabase container instance
 * @param environment ApplicationEnvironment
 */
fun KoinApplication.loadKoinModules(environment: ApplicationEnvironment) {
        val connectionString = environment.config.property("azure.service_bus.connectionString").getString()
        val topicName = environment.config.property("azure.service_bus.topicName").getString()
        val subscriptionName = environment.config.property("azure.service_bus.subscriptionName").getString()
        val sharedAccessKeyName = environment.config.property("azure.service_bus.sharedAccessKeyName").getString()
        val sharedAccessKey = environment.config.property("azure.service_bus.sharedAccessKey").getString()

        val accountName = environment.config.property("azure.blob_storage.accountName").getString()
        val accountKey = environment.config.property("azure.blob_storage.accountKey").getString()
        val containerName = environment.config.property("azure.blob_storage.containerName").getString()

     CamelProcessor().sinkAsbTopicsToBlob(connectionString, topicName, subscriptionName, sharedAccessKeyName, sharedAccessKey, accountName, accountKey,containerName)


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

}
