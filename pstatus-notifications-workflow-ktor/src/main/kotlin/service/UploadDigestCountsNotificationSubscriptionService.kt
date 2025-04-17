package gov.cdc.ocio.processingnotifications.service

import gov.cdc.ocio.processingnotifications.activity.NotificationActivitiesImpl
import gov.cdc.ocio.processingnotifications.model.UploadDigestSubscription
import gov.cdc.ocio.processingnotifications.model.WorkflowSubscriptionResult
import gov.cdc.ocio.processingnotifications.temporal.WorkflowEngine
import gov.cdc.ocio.processingnotifications.workflow.digestcounts.UploadDigestCountsNotificationWorkflow
import gov.cdc.ocio.processingnotifications.workflow.digestcounts.UploadDigestCountsNotificationWorkflowImpl
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
     * @param subscription UploadDigestSubscription
     * @return WorkflowSubscriptionResult
     */
    fun run(
        subscription: UploadDigestSubscription
    ): WorkflowSubscriptionResult {

        val numDaysAgoToRun = subscription.numDaysAgoToRun
        val dataStreamIds = subscription.dataStreamIds
        val dataStreamRoutes = subscription.dataStreamRoutes
        val jurisdictions = subscription.jurisdictions
        val cronSchedule = subscription.cronSchedule
        val emailAddresses = subscription.emailAddresses
        val taskQueue = "uploadDigestCountsTaskQueue"

        val workflow = workflowEngine.setupWorkflow(
            description,
            taskQueue,
            cronSchedule,
            UploadDigestCountsNotificationWorkflowImpl::class.java,
            notificationActivitiesImpl,
            UploadDigestCountsNotificationWorkflow::class.java
        )

        val execution = WorkflowClient.start(
            workflow::processDailyUploadDigest,
            numDaysAgoToRun,
            dataStreamIds,
            dataStreamRoutes,
            jurisdictions,
            emailAddresses
        )

        val workflowId = execution.workflowId
        return WorkflowSubscriptionResult(
            subscriptionId = workflowId,
            message = "Successfully subscribed for $workflowId",
            emailAddresses = emailAddresses,
            webhookUrl = ""
        )
    }
}