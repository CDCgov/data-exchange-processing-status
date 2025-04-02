package gov.cdc.ocio.messagesystem.utils

import gov.cdc.ocio.messagesystem.MessageProcessorConfig
import io.ktor.server.application.*
import io.ktor.server.config.*
import org.koin.core.module.Module
import org.koin.dsl.module


object MessageProcessorConfigKoinCreator {

    /**
     * Creates the message processor configuration from the provided application environment.
     *
     * @param environment ApplicationEnvironment
     * @return MessageProcessorConfig
     */
    private fun createMessageProcessorConfig(
        environment: ApplicationEnvironment
    ): MessageProcessorConfig {
        val forwardValidatedReports = environment.config
            .tryGetString("ktor.message_processor.forward_validated_reports")
            ?.toBooleanStrictOrNull() ?: false
        return MessageProcessorConfig(
            forwardValidatedReports = forwardValidatedReports
        )
    }

    /**
     * Creates the message processor config module from the application environment.
     *
     * @param environment ApplicationEnvironment
     * @return Module
     */
    fun moduleFromAppEnv(environment: ApplicationEnvironment): Module {
        return module {
            single { createMessageProcessorConfig(environment) }
        }
    }
}

