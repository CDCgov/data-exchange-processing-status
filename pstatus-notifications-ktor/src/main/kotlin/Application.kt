package gov.cdc.ocio.processingstatusnotifications



import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.routing.*
/*fun KoinApplication.loadKoinModules(environment: ApplicationEnvironment): KoinApplication {

}*/

fun main(args: Array<String>) {
    embeddedServer(Netty, commandLineEnvironment(args)).start(wait = true)
}

fun Application.module() {
   // graphQLModule()
    install(ContentNegotiation) {
        json()
    }
    routing {
        subscribeEmailNotificationRoute()
        unsubscribeEmailNotificationRoute()
    }

}
