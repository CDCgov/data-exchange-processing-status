package gov.cdc.ocio.processingnotifications.service

import gov.cdc.ocio.processingnotifications.activity.NotificationActivitiesImpl
import gov.cdc.ocio.processingnotifications.temporal.WorkflowEngine
import gov.cdc.ocio.processingnotifications.workflow.digestcounts.UploadDigestCountsNotificationWorkflow
import gov.cdc.ocio.processingnotifications.workflow.digestcounts.UploadDigestCountsNotificationWorkflowImpl
import gov.cdc.ocio.types.model.WorkflowSubscription
import gov.cdc.ocio.types.model.WorkflowSubscriptionResult
import io.grpc.StatusRuntimeException
import io.temporal.client.WorkflowClient
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


/**
 * The main class which subscribes the workflow execution for daily upload digest counts.
 *
 * @property logger KotlinLogging.Logger
 * @property workflowEngine WorkflowEngine
 * @property notificationActivitiesImpl  NotificationActivitiesImpl
 */
class UploadDigestCountsNotificationSubscriptionService: KoinComponent {

    private val logger = KotlinLogging.logger {}

    private val workflowEngine by inject<WorkflowEngine>()

    private val notificationActivitiesImpl = NotificationActivitiesImpl()

    private val description =
        """
        Provides a digest of the upload counts for the data streams and day provided.
        """.trimIndent()

    /**
     * The main method which executes workflow for orchestrating the daily digest counts.
     *
     * @param subscription WorkflowSubscription
     * @return WorkflowSubscriptionResult
     * @throws IllegalStateException
     * @throws StatusRuntimeException
     */
    @Throws(IllegalStateException::class, StatusRuntimeException::class)
    fun run(
        subscription: WorkflowSubscription
    ): WorkflowSubscriptionResult {
        val taskQueue = "uploadDigestCountsTaskQueue"

        val workflow = workflowEngine.setupWorkflow(
            description,
            taskQueue,
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
            subscriptionId = execution.workflowId,
            message = "Successfully subscribed",
            emailAddresses = subscription.emailAddresses,
            webhookUrl = subscription.webhookUrl
        )
    }
}