package gov.cdc.ocio.processingnotifications.workflow

import gov.cdc.ocio.processingnotifications.activity.NotificationActivities
import io.temporal.activity.ActivityOptions
import io.temporal.common.RetryOptions
import io.temporal.workflow.Workflow
import mu.KotlinLogging
import java.time.Duration


/**
 * The implementation class for errors on missing fields from an upload.
 *
 * @property activities T
 */
class UploadErrorsNotificationWorkflowImpl : UploadErrorsNotificationWorkflow {
    private val logger = KotlinLogging.logger {}

    private val activities = Workflow.newActivityStub(
        NotificationActivities::class.java,
        ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(10)) // Set the start-to-close timeout
            .setScheduleToCloseTimeout(Duration.ofMinutes(1)) // Set the schedule-to-close timeout
            .setRetryOptions(
                RetryOptions.newBuilder()
                    .setMaximumAttempts(3) // Set retry options if needed
                    .build()
            )
            .build()
    )

    /**
     * The function which gets invoked by the temporal WF engine and which checks for the errors in the upload and
     * invokes the activity, if there are errors.
     *
     * @param dataStreamId String
     * @param dataStreamRoute String
     * @param jurisdiction String
     * @param daysToRun List<String>
     * @param timeToRun String
     * @param deliveryReference String
     */
    override fun checkUploadErrorsAndNotify(
        dataStreamId: String,
        dataStreamRoute: String,
        jurisdiction: String,
        daysToRun: List<String>,
        timeToRun: String,
        deliveryReference: String
    ) {
        try {
            // Logic to check if the upload occurred
            val error = checkUploadErrors(dataStreamId, dataStreamRoute, jurisdiction)
            if (error.isNotEmpty()) {
                activities.sendUploadErrorsNotification(error, deliveryReference)
            }
        } catch (e: Exception) {
            logger.error("Error occurred while checking for errors in upload. Errors are : ${e.message}")
        }
    }

    override fun cancelWorkflow() {
        logger.info("workflow canceled")
    }

    /**
     * The actual function which checks for errors in the fields used for upload.
     *
     * @param dataStreamId String
     * @param dataStreamRoute String
     * @param jurisdiction String
     */
    private fun checkUploadErrors(dataStreamId: String, dataStreamRoute: String, jurisdiction: String): String {
        var error = ""
        if (dataStreamId.isEmpty()) {
            error = "DataStreamId is missing from the upload."
        }
        if (dataStreamRoute.isEmpty()) {
            error += "DataStreamRoute is missing from the upload."
        }
        if (jurisdiction.isEmpty()) {
            error += "Jurisdiction is missing from the upload"
        }
        return error
    }
}
