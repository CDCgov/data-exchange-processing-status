package gov.cdc.ocio.processingnotifications.service

import gov.cdc.ocio.processingnotifications.activity.NotificationActivities
import gov.cdc.ocio.processingnotifications.activity.NotificationActivitiesImpl
import gov.cdc.ocio.processingnotifications.model.WorkflowTaskQueue
import gov.cdc.ocio.processingnotifications.temporal.WorkflowEngine
import gov.cdc.ocio.processingnotifications.workflow.deadlinecheck.DeadlineCheckNotificationWorkflow
import gov.cdc.ocio.processingnotifications.workflow.deadlinecheck.DeadlineCheckNotificationWorkflowImpl
import gov.cdc.ocio.processingnotifications.workflow.digestcounts.UploadDigestCountsNotificationWorkflow
import gov.cdc.ocio.processingnotifications.workflow.digestcounts.UploadDigestCountsNotificationWorkflowImpl
import gov.cdc.ocio.processingnotifications.workflow.toperrors.DataStreamTopErrorsNotificationWorkflow
import gov.cdc.ocio.processingnotifications.workflow.toperrors.DataStreamTopErrorsNotificationWorkflowImpl
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

    fun subscribeTopErrors(subscription: WorkflowSubscriptionForDataStreams): WorkflowSubscriptionResult {
        val workflow = workflowEngine.setupWorkflow(
            "Determines the count of the top 5 errors that have occurred for this data stream in the time range provided.",
            WorkflowTaskQueue.TOP_ERRORS.toString(),
            subscription.cronSchedule,
            DataStreamTopErrorsNotificationWorkflowImpl::class.java,
            notificationActivitiesImpl,
            DataStreamTopErrorsNotificationWorkflow::class.java
        )

        val execution = WorkflowClient.start(
            workflow::checkDataStreamTopErrorsAndNotify,
            subscription
        )

        return WorkflowSubscriptionResult(
            subscriptionId = execution.workflowId
        )
    }

    fun subscribeUploadDigest(subscription: WorkflowSubscriptionForDataStreams): WorkflowSubscriptionResult {
        val workflow = workflowEngine.setupWorkflow(
            "Provides a digest of the upload counts for the data streams and day provided.",
            WorkflowTaskQueue.UPLOAD_DIGEST.toString(),
            subscription.cronSchedule,
            UploadDigestCountsNotificationWorkflowImpl::class.java,
            notificationActivitiesImpl,
            UploadDigestCountsNotificationWorkflow::class.java
        )

        val execution = WorkflowClient.start(
            workflow::processDailyUploadDigest,
            subscription
        )

        return WorkflowSubscriptionResult(
            subscriptionId = execution.workflowId
        )
    }

    fun subscribeDeadlineCheck(subscription: WorkflowSubscriptionDeadlineCheck): WorkflowSubscriptionResult {
        val workflow = workflowEngine.setupWorkflow(
            "Checks to see if all the expected uploads have occurred by the deadline provided.",
            WorkflowTaskQueue.DEADLINE_CHECK.toString(),
            subscription.cronSchedule,
            DeadlineCheckNotificationWorkflowImpl::class.java,
            notificationActivitiesImpl,
            DeadlineCheckNotificationWorkflow::class.java
        )

        val execution = WorkflowClient.start(
            workflow::checkUploadDeadlinesAndNotify,
            subscription
        )

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