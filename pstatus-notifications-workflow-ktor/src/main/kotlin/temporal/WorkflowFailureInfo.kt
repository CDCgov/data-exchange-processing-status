package gov.cdc.ocio.processingnotifications.temporal

import io.temporal.api.enums.v1.RetryState
import io.temporal.api.enums.v1.TimeoutType

/**
 * Represents detailed information regarding a workflow failure.
 *
 * @property source Specifies the source of the failure. Possible values include:
 *                  "continued-as-new", "current-run", "timed-out", "canceled".
 * @property failureMessage Provides the message describing the failure, if available.
 * @property causeMessage Describes the root cause of the failure, if available.
 * @property stackTrace Contains the stack trace related to the failure for debugging purposes, if available.
 * @property activityName Specifies the name of the activity associated with the failure, if applicable.
 * @property retryState Indicates the state of the retry mechanism at the time of failure, if applicable.
 * @property timeoutType Describes the type of timeout that caused the failure, if any.
 */
data class WorkflowFailureInfo(
    val source: String, // "continued-as-new", "current-run", "timed-out", "canceled"
    val failureMessage: String?,
    val causeMessage: String?,
    val stackTrace: String?,
    val activityName: String? = null,
    val retryState: RetryState? = null,
    val timeoutType: TimeoutType? = null
)