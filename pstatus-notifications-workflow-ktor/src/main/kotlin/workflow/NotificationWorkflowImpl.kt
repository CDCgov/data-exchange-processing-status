package gov.cdc.ocio.processingnotifications.workflow

import gov.cdc.ocio.types.model.WorkflowSubscriptionDeadlineCheck
import gov.cdc.ocio.types.model.WorkflowSubscriptionForDataStreams
import org.koin.core.component.KoinComponent

class NotificationWorkflowImpl : NotificationWorkflow, KoinComponent {
    override fun notifyUploadDeadlines(sub: WorkflowSubscriptionDeadlineCheck) {
        TODO("Not yet implemented")
    }

    override fun notifyUploadDigest(sub: WorkflowSubscriptionForDataStreams) {
        TODO("Not yet implemented")
    }

    override fun notifyDataStreamTopErrors(sub: WorkflowSubscriptionForDataStreams) {
        TODO("Not yet implemented")
    }
}