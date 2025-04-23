package gov.cdc.ocio.notificationdispatchers

import io.ktor.server.application.*
import org.koin.core.module.Module
import org.koin.dsl.module


object NotificationDispatcherKoinCreator {

    /**
     * Creates a koin module and injects a singleton for the email sender from the [ApplicationEnvironment]
     *
     * @param environment ApplicationEnvironment
     * @return Module
     */
    fun moduleFromAppEnv(environment: ApplicationEnvironment): Module {
        val dispatcherModule = module {
            single(createdAtStart = true) { NotificationDispatcher(environment) }
        }
        return dispatcherModule
    }
}