package gov.cdc.ocio.processingnotifications.workflow.digestcounts

/**
 * Model for counts of upload status and file delivery.
 *
 * @property uploadStarted Int
 * @property uploadCompleted Int
 * @property deliveryFailed Int
 * @property deliverySucceeded Int
 * @constructor
 */
data class Counts(
    val uploadStarted: Int,
    val uploadCompleted: Int,
    val deliveryFailed: Int,
    val deliverySucceeded: Int
)