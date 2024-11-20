package gov.cdc.ocio.processingnotifications


import gov.cdc.ocio.database.utils.DatabaseKoinCreator
import gov.cdc.ocio.processingnotifications.model.UploadDigestSubscription
import gov.cdc.ocio.processingnotifications.service.UploadDigestCountsNotificationSubscriptionService
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import org.koin.core.KoinApplication
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

fun KoinApplication.loadKoinModules(environment: ApplicationEnvironment): KoinApplication {
    val databaseModule = DatabaseKoinCreator.moduleFromAppEnv(environment)
    val healthModule = module {
        single {
            val appConfig: ApplicationConfig = get()
            appConfig.property("temporal.temporal_service_target").getString()
        }
    }
    return modules(listOf(databaseModule, healthModule, module {single{environment.config}}))
}


fun main(args: Array<String>) {
    embeddedServer(Netty, commandLineEnvironment(args)).start(wait = true)
}

fun Application.module() {
    install(Koin) {
        loadKoinModules(environment)
    //    modules(healthModule, module { single { environment.config } } )

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

    //***ONLY FOR QUICK AND DIRTY TESTING***
    //  testUploadDigestCount()

}

/**
 * THIS IS ONLY FOR QUICK AND DIRTY TESTING. CAN BE REMOVED ONCE THIS SERVICE IS MATURED
 */
fun testUploadDigestCount(){
    val service = UploadDigestCountsNotificationSubscriptionService()
    service.run(
        UploadDigestSubscription(
            jurisdictionIds = listOf("SMOKE", "SMOKE100"),
            dataStreamIds = listOf("dex-testing", "dex-testing100"),
            listOf("Mon","Tue","Wed","Thu","Fri","Sat","Sun"),
            "45 02 * *",
            "xph6@cdc.gov"
        ))

}