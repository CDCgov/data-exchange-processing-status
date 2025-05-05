package gov.cdc.ocio.processingnotifications.workflow

import gov.cdc.ocio.types.model.WorkflowSubscriptionDeadlineCheck
import gov.cdc.ocio.types.model.WorkflowSubscriptionForDataStreams
import io.temporal.workflow.WorkflowInterface
import io.temporal.workflow.WorkflowMethod

@WorkflowInterface
interface NotificationWorkflow {
    @WorkflowMethod
    fun notifyUploadDeadlines(sub: WorkflowSubscriptionDeadlineCheck)
    @WorkflowMethod
    fun notifyUploadDigest(sub: WorkflowSubscriptionForDataStreams)
    @WorkflowMethod
    fun notifyDataStreamTopErrors(sub: WorkflowSubscriptionForDataStreams)
}