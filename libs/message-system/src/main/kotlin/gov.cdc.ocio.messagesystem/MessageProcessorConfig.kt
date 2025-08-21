package gov.cdc.ocio.messagesystem


/**
 * Message processor configuration.  Eventually this will contain other attributes such as the strictness of the
 * validation.
 *
 * @property forwardValidatedReports Boolean
 * @constructor
 */
data class MessageProcessorConfig(
    val forwardValidatedReports: Boolean
)