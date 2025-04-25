package gov.cdc.ocio.messagesystem.models

import io.ktor.server.application.*
import io.ktor.server.config.*
import mu.KotlinLogging


/**
 * Message system types
 */
enum class MessageSystemType {
    AWS,
    AZURE_SERVICE_BUS,
    RABBITMQ;

    companion object {
        /**
         * Retrieves the message system from the app environment and translates that into a message system enum if possible.
         *
         * @param environment ApplicationEnvironment
         * @return MessageSystemType?
         */
        fun getFromAppEnv(environment: ApplicationEnvironment): MessageSystemType? {
            val logger = KotlinLogging.logger {}

            // Determine which messaging system module to load
            val currentMessagingSystem = environment.config.tryGetString("ktor.message_system") ?: ""
            val messageSystemType: MessageSystemType? = try {
                MessageSystemType.valueOf(currentMessagingSystem.uppercase())
            } catch (e: IllegalArgumentException) {
                logger.error("Unrecognized message system: $currentMessagingSystem")
                null
            }
            return messageSystemType
        }
    }
}