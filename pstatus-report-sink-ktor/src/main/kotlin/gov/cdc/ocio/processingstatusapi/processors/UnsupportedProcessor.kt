package gov.cdc.ocio.processingstatusapi.processors

import gov.cdc.ocio.messagesystem.MessageProcessorInterface
import mu.KotlinLogging


/**
 * Unsupported processor implementation.
 *
 * @property logger KLogger
 */
class UnsupportedProcessor: MessageProcessorInterface {

    private val logger = KotlinLogging.logger {}

    /**
     * The processMessage implementation of unsupported processors will simply log an error.
     *
     * @param message String
     */
    override fun processMessage(message: String) {
        logger.error { "Unhandled message: $message" }
    }
}