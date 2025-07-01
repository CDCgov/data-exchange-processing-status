package gov.cdc.ocio.processingstatusnotifications.rulesEngine

/**
 * Represents the result of an MVEL expression validation operation.
 *
 * @property isValid Indicates whether the MVEL expression is valid.
 * @property errorMessage Provides details about the validation error if the expression is not valid, or null if there
 * are no errors.
 */
data class MvelValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
)