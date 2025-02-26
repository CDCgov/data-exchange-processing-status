package gov.cdc.ocio.processingnotifications.service

import gov.cdc.ocio.processingnotifications.activity.NotificationActivitiesImpl
import gov.cdc.ocio.processingnotifications.model.UploadDigestSubscription
import gov.cdc.ocio.processingnotifications.model.WorkflowSubscriptionResult
import gov.cdc.ocio.processingnotifications.temporal.WorkflowEngine
import gov.cdc.ocio.processingnotifications.workflow.UploadDigestCountsNotificationWorkflow
import gov.cdc.ocio.processingnotifications.workflow.UploadDigestCountsNotificationWorkflowImpl
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

    /**
     * The main method which executes workflow for orchestrating the daily digest counts.
     *
     * @param subscription UploadDigestSubscription
     * @return WorkflowSubscriptionResult
     */
    fun run(subscription: UploadDigestSubscription):
            WorkflowSubscriptionResult {
        try {
            val dataStreams= subscription.dataStreamIds
            val jurisdictionIds = subscription.jurisdictionIds
            val daysToRun = subscription.daysToRun
            val timeToRun = subscription.timeToRun
            val deliveryReference= subscription.deliveryReference
            val taskQueue = "uploadDigestCountsTaskQueue"
            val workflow = workflowEngine.setupWorkflow(
                taskQueue,daysToRun,timeToRun,
                UploadDigestCountsNotificationWorkflowImpl::class.java,
                notificationActivitiesImpl,
                UploadDigestCountsNotificationWorkflow::class.java
            )

            workflow?.let {
                val execution = WorkflowClient.start(
                    workflow::processDailyUploadDigest,
                    jurisdictionIds,
                    dataStreams,
                    deliveryReference
                )

                val workflowId = execution.workflowId
                return WorkflowSubscriptionResult(
                    subscriptionId = workflowId,
                    message = "Successfully subscribed for $workflowId",
                    deliveryReference = deliveryReference
                )
            }
        }
        catch (e:Exception){
            logger.error("Error occurred while subscribing workflow for daily upload digest counts: ${e.message}")
        }
        throw Exception("Error occurred while executing workflow engine to subscribe for daily upload digest counts")
    }
}