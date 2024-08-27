package gov.cdc.ocio.processingnotifications.workflow

import gov.cdc.ocio.processingnotifications.activity.NotificationActivities
import io.temporal.activity.ActivityOptions
import io.temporal.common.RetryOptions
import io.temporal.workflow.Workflow
import java.time.Duration

class UploadErrorsNotificationWorkflowImpl : UploadErrorsNotificationWorkflow {
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

    override fun checkUploadErrorsAndNotify(
        dataStreamId: String,
        dataStreamRoute: String,
        jurisdiction: String,
        daysToRun: List<String>,
        timeToRun: String,
        deliveryReference: String
    ) {
        try {
            // Logic to check if the upload occurred*/
            val error = checkUploadErrors(dataStreamId, dataStreamRoute, jurisdiction)
            if (error.isNotEmpty()) {
                activities.sendUploadErrorsNotification(error,deliveryReference)
            }
        } catch (e: Exception) {
        }
    }

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
