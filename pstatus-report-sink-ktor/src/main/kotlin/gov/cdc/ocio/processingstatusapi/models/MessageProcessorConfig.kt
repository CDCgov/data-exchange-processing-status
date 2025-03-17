package gov.cdc.ocio.processingstatusapi.models


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