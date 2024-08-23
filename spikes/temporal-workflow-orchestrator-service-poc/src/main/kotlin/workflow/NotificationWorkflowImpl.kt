package gov.cdc.ocio.processingnotifications.workflow

import gov.cdc.ocio.processingnotifications.activity.NotificationActivities
import gov.cdc.ocio.processingnotifications.cache.InMemoryCacheService
import io.temporal.activity.ActivityOptions
import io.temporal.common.RetryOptions
import io.temporal.workflow.Workflow
import java.time.Duration

class NotificationWorkflowImpl : NotificationWorkflow {
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

    override fun checkUploadAndNotify(
        dataStreamId: String,
        dataStreamRoute: String,
        jurisdiction: String,
        daysToRun: List<String>,
        timeToRun: String,
        deliveryReference: String
    ) {

        try {
            // Logic to check if the upload occurred*/
            val uploadOccurred = checkUpload(dataStreamId, jurisdiction)
            if (!uploadOccurred) {
                activities.sendNotification(dataStreamId, dataStreamRoute, jurisdiction, deliveryReference)
            }
        } catch (e: Exception) {
        }

    }

    private fun checkUpload(dataStreamId: String, jurisdiction: String): Boolean {
        // add check logic here
        return false
    }
}
