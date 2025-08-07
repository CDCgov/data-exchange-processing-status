package gov.cdc.ocio.processingnotifications.temporal

import io.temporal.api.common.v1.WorkflowExecution
import io.temporal.api.enums.v1.EventType
import io.temporal.api.failure.v1.Failure
import io.temporal.api.workflowservice.v1.GetWorkflowExecutionHistoryRequest
import io.temporal.serviceclient.WorkflowServiceStubs

/**
 * Retrieves detailed information about a workflow failure, if any occurred.
 * This method analyzes the workflow execution history and identifies the source and type of failure.
 *
 * @param service The WorkflowServiceStubs instance used to fetch the workflow execution history.
 * @param namespace The namespace of the workflow execution.
 * @return A WorkflowFailureInfo object containing details about the failure, or null if no failure is found.
 */
fun WorkflowExecution.getWorkflowFailureInfo(
    service: WorkflowServiceStubs,
    namespace: String
): WorkflowFailureInfo? {

    val history = service.blockingStub().getWorkflowExecutionHistory(
        GetWorkflowExecutionHistoryRequest.newBuilder()
            .setNamespace(namespace)
            .setExecution(this)
            .build()
    )

    val events = history.history.eventsList

    // Case 1: continued-as-new failure from previous run
    val startEvent = events.firstOrNull { it.eventType == EventType.EVENT_TYPE_WORKFLOW_EXECUTION_STARTED }
    val continuedFailure = startEvent
        ?.workflowExecutionStartedEventAttributes
        ?.continuedFailure

    if (continuedFailure != null) {
        return WorkflowFailureInfo(
            source = "continued-as-new",
            failureMessage = continuedFailure.message,
            causeMessage = continuedFailure.cause?.message,
            stackTrace = continuedFailure.stackTrace,
            activityName = continuedFailure.activityFailureInfo?.activityType?.name,
            retryState = continuedFailure.activityFailureInfo?.retryState,
            timeoutType = continuedFailure.cause?.timeoutFailureInfo?.timeoutType
        )
    }

    // Case 2: current run failed
    val failedEvent = events.firstOrNull { it.eventType == EventType.EVENT_TYPE_WORKFLOW_EXECUTION_FAILED }
    val failedAttrs = failedEvent?.workflowExecutionFailedEventAttributes
    if (failedAttrs != null) {
        val failure: Failure = failedAttrs.failure
        return WorkflowFailureInfo(
            source = "current-run",
            failureMessage = failure.message,
            causeMessage = failure.cause?.message,
            stackTrace = failure.stackTrace
        )
    }

    // Case 3: workflow timed out
    val timeoutEvent = events.firstOrNull { it.eventType == EventType.EVENT_TYPE_WORKFLOW_EXECUTION_TIMED_OUT }
    val timeoutAttrs = timeoutEvent?.workflowExecutionTimedOutEventAttributes
    if (timeoutAttrs != null) {
        return WorkflowFailureInfo(
            source = "timed-out",
            failureMessage = "Workflow timed out after reaching run or execution timeout.",
            causeMessage = null,
            stackTrace = null
        )
    }


    // Case 4: workflow canceled
    val canceledEvent = events.firstOrNull { it.eventType == EventType.EVENT_TYPE_WORKFLOW_EXECUTION_CANCELED }
    val canceledAttrs = canceledEvent?.workflowExecutionCanceledEventAttributes
    if (canceledAttrs != null) {
        val detailsString = canceledAttrs.details?.payloadsList
            ?.joinToString("; ") { it.data.toStringUtf8() } // decode payloads safely

        return WorkflowFailureInfo(
            source = "canceled",
            failureMessage = "Workflow was canceled.",
            causeMessage = detailsString,
            stackTrace = null
        )
    }

    return null // No failure, timeout, or cancellation
}
