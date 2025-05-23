package gov.cdc.ocio.processingnotifications.service

import gov.cdc.ocio.processingnotifications.workflow.deadlinecheck.DeadlineCheckNotificationActivitiesImpl
import gov.cdc.ocio.processingnotifications.model.WorkflowTaskQueue
import gov.cdc.ocio.processingnotifications.temporal.WorkflowEngine
import gov.cdc.ocio.processingnotifications.workflow.deadlinecheck.DeadlineCheckNotificationWorkflow
import gov.cdc.ocio.processingnotifications.workflow.deadlinecheck.DeadlineCheckNotificationWorkflowImpl
import gov.cdc.ocio.processingnotifications.workflow.digestcounts.UploadDigestCountsNotificationActivitiesImpl
import gov.cdc.ocio.processingnotifications.workflow.digestcounts.UploadDigestCountsNotificationWorkflow
import gov.cdc.ocio.processingnotifications.workflow.digestcounts.UploadDigestCountsNotificationWorkflowImpl
import gov.cdc.ocio.processingnotifications.workflow.toperrors.TopErrorsNotificationWorkflow
import gov.cdc.ocio.processingnotifications.workflow.toperrors.TopErrorsNotificationWorkflowImpl
import gov.cdc.ocio.processingnotifications.workflow.toperrors.TopErrorsNotificationActivitiesImpl
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

    fun subscribeTopErrors(subscription: WorkflowSubscriptionForDataStreams): WorkflowSubscriptionResult {
        val workflow = workflowEngine.setupWorkflow(
            "Determines the count of the top 5 errors that have occurred for this data stream in the time range provided.",
            WorkflowTaskQueue.TOP_ERRORS.toString(),
            subscription.cronSchedule,
            TopErrorsNotificationWorkflowImpl::class.java,
            TopErrorsNotificationActivitiesImpl(),
            TopErrorsNotificationWorkflow::class.java
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
            UploadDigestCountsNotificationActivitiesImpl(),
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
            DeadlineCheckNotificationActivitiesImpl(),
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