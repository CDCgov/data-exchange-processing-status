package gov.cdc.ocio.processingnotifications

import gov.cdc.ocio.database.DatabaseType
import gov.cdc.ocio.database.cosmos.CosmosRepository
import gov.cdc.ocio.database.couchbase.CouchbaseRepository
import gov.cdc.ocio.database.dynamo.DynamoRepository
import gov.cdc.ocio.database.persistence.ProcessingStatusRepository
import gov.cdc.ocio.processingnotifications.model.UploadDigestSubscription
import gov.cdc.ocio.processingnotifications.service.UploadDigestCountsNotificationSubscriptionService
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import mu.KotlinLogging
import org.koin.core.KoinApplication
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

fun KoinApplication.loadKoinModules(environment: ApplicationEnvironment): KoinApplication {
  /*  val cosmosModule = module {
        val uri = environment.config.property("azure.cosmos_db.client.endpoint").getString()
        val authKey = environment.config.property("azure.cosmos_db.client.key").getString()
        single<ProcessingStatusRepository>{ CosmosRepository(uri, authKey) } //createdAtStart = true is not working , with lib integration
        // Create a CosmosDB config that can be dependency injected (for health checks)
        single(createdAtStart = true) { CosmosConfiguration(uri, authKey) }
    }*/

    //return modules(listOf(cosmosModule))
     val logger = KotlinLogging.logger {}
    val databaseModule = module {
        when (val database = environment.config.tryGetString("ktor.database")) {

            DatabaseType.COUCHBASE.value -> {
                val connectionString = environment.config.property("couchbase.connection_string").getString()
                single<ProcessingStatusRepository> {
                    CouchbaseRepository(connectionString, "admin", "password")
                }
            }
            DatabaseType.COSMOS.value -> {
                val uri = environment.config.property("azure.cosmos_db.client.endpoint").getString()
                val authKey = environment.config.property("azure.cosmos_db.client.key").getString()
                single<ProcessingStatusRepository> {
                    CosmosRepository(uri, authKey, "/uploadId")
                }
            }
            DatabaseType.DYNAMO.value -> {
                val dynamoTablePrefix = environment.config.property("aws.dynamo.table_prefix").getString()
                single<ProcessingStatusRepository> {
                    DynamoRepository(dynamoTablePrefix)
                }
            }
            else -> logger.error("Unsupported database requested: $database")
        }
    }
    return modules(listOf(databaseModule))
}
fun main(args: Array<String>) {
    embeddedServer(Netty, commandLineEnvironment(args)).start(wait = true)
}

fun Application.module() {
    install(Koin) {
        loadKoinModules(environment)
    }
    install(ContentNegotiation) {
        jackson()
    }
    routing {
        subscribeDeadlineCheckRoute()
        unsubscribeDeadlineCheck()
        subscribeUploadErrorsNotification()
        unsubscribeUploadErrorsNotification()
        subscribeDataStreamTopErrorsNotification()
        unsubscribesDataStreamTopErrorsNotification()
        subscribeUploadDigestCountsRoute()
        unsubscribeUploadDigestCountsRoute()
        healthCheckRoute()
    }

    testUploadDigestCount()

}

fun testUploadDigestCount(){
    val service = UploadDigestCountsNotificationSubscriptionService()
    service.run(
        UploadDigestSubscription(
        jurisdictionIds = listOf("SMOKE", "J456"),
        dataStreamIds = listOf("dex-testing", "dex-testing100"),
            listOf("Mon","Tue","Wed","Thu","Fri","Sat","Sun"),
            "35 02 * *",
           "xph6@cdc.gov"
    ))

}