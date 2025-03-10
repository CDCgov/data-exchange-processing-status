package gov.cdc.ocio.processingnotifications.service

import gov.cdc.ocio.processingnotifications.activity.NotificationActivitiesImpl
import gov.cdc.ocio.processingnotifications.model.UploadErrorsNotificationSubscription
import gov.cdc.ocio.processingnotifications.model.WorkflowSubscriptionResult
import gov.cdc.ocio.processingnotifications.temporal.WorkflowEngine
import gov.cdc.ocio.processingnotifications.workflow.UploadErrorsNotificationWorkflow
import gov.cdc.ocio.processingnotifications.workflow.UploadErrorsNotificationWorkflowImpl
import io.temporal.client.WorkflowClient
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


/**
 * The main class which subscribes the workflow execution for upload errors.
 *
 * @property workflowEngine WorkflowEngine
 * @property notificationActivitiesImpl NotificationActivitiesImpl
 * @property logger KLogger
 */
class UploadErrorsNotificationSubscriptionService : KoinComponent {

    private val logger = KotlinLogging.logger {}

    private val workflowEngine by inject<WorkflowEngine>()

    private val notificationActivitiesImpl = NotificationActivitiesImpl()

    private val description =
        """
        Provides the upload errors for the data stream and date/time range provided.
        """.trimIndent()

    /**
     * The main method which executes workflow engine to check for upload errors and notify.
     *
     * @param subscription UploadErrorsNotificationSubscription
     * @return WorkflowSubscriptionResult
     */
    fun run(
        subscription: UploadErrorsNotificationSubscription
    ): WorkflowSubscriptionResult {

        val dataStreamId = subscription.dataStreamId
        val dataStreamRoute = subscription.dataStreamRoute
        val jurisdiction = subscription.jurisdiction
        val cronSchedule = subscription.cronSchedule
        val deliveryReference = subscription.deliveryReference
        val taskQueue = "uploadErrorsNotificationTaskQueue"

        val workflow = workflowEngine.setupWorkflow(
            description,
            taskQueue,
            cronSchedule,
            UploadErrorsNotificationWorkflowImpl::class.java,
            notificationActivitiesImpl,
            UploadErrorsNotificationWorkflow::class.java
        )

        val execution = WorkflowClient.start(
            workflow::checkUploadErrorsAndNotify,
            dataStreamId,
            dataStreamRoute,
            jurisdiction,
            cronSchedule,
            deliveryReference
        )

        logger.info("Started workflow with id: ${execution.workflowId}")

        return WorkflowSubscriptionResult(
            subscriptionId = execution.workflowId,
            message = "",
            deliveryReference = ""
        )
    }
}