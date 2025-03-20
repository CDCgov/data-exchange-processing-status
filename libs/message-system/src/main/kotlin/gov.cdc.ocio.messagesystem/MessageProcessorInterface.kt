package gov.cdc.ocio.messagesystem

/**
 * Defines the common interfaces for all message processors.
 */
interface MessageProcessorInterface {

    /**
     * Process the provided message as a string.
     *
     * @param message String
     */
    fun processMessage(message: String)
}