package gov.cdc.ocio.processingnotifications.service

import gov.cdc.ocio.processingnotifications.activity.NotificationActivities
import gov.cdc.ocio.processingnotifications.activity.NotificationActivitiesImpl
import gov.cdc.ocio.processingnotifications.model.TemporalSubscription
import gov.cdc.ocio.processingnotifications.model.WorkflowTaskQueue
import gov.cdc.ocio.processingnotifications.temporal.WorkflowEngine
import gov.cdc.ocio.processingnotifications.workflow.NotificationWorkflow
import gov.cdc.ocio.processingnotifications.workflow.NotificationWorkflowImpl
import gov.cdc.ocio.types.model.WorkflowSubscription
import gov.cdc.ocio.types.model.WorkflowSubscriptionDeadlineCheck
import gov.cdc.ocio.types.model.WorkflowSubscriptionForDataStreams
import gov.cdc.ocio.types.model.WorkflowSubscriptionResult
import io.temporal.client.WorkflowClient
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class NotificationSubscriptionService: KoinComponent {
    private val logger = KotlinLogging.logger {}
    private val workflowEngine by inject<WorkflowEngine>()
    private val notificationActivitiesImpl = NotificationActivitiesImpl()

    fun <T : WorkflowSubscription> run(
        subscription: TemporalSubscription<T>,
    ): WorkflowSubscriptionResult {
        val workflow = workflowEngine.setupWorkflow(
            subscription.description,
            subscription.taskQueue.toString(),
            subscription.workflowSubscription.cronSchedule,
            NotificationWorkflowImpl::class.java,
            notificationActivitiesImpl,
            NotificationWorkflow::class.java
        )

        val execution = when(subscription.taskQueue) {
            WorkflowTaskQueue.TOP_ERRORS -> WorkflowClient.start(
                workflow::notifyDataStreamTopErrors,
                subscription.workflowSubscription as WorkflowSubscriptionForDataStreams)
            WorkflowTaskQueue.UPLOAD_DIGEST -> WorkflowClient.start(
                workflow::notifyUploadDigest,
                subscription.workflowSubscription as WorkflowSubscriptionForDataStreams)
            WorkflowTaskQueue.DEADLINE_CHECK -> WorkflowClient.start(
                workflow::notifyUploadDeadlines,
                subscription.workflowSubscription as WorkflowSubscriptionDeadlineCheck)
        }

        // TODO log success message
        return WorkflowSubscriptionResult(
            subscriptionId = execution.workflowId
        )
    }

    fun unsubscribe(subscriptionId: String) : WorkflowSubscriptionResult {
        try {
            workflowEngine.cancelWorkflow(subscriptionId)
            return WorkflowSubscriptionResult(
                subscriptionId = subscriptionId
            )
        } catch (ex: Exception) {
            logger.error("Error occurred while unsubscribing and canceling the workflow for workflowId $subscriptionId: ${ex.message}")
            throw ex
        }
    }
}