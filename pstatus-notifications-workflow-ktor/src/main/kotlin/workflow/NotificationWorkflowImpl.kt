package gov.cdc.ocio.processingnotifications.workflow

import gov.cdc.ocio.processingnotifications.activity.NotificationActivities
import io.temporal.activity.ActivityOptions
import io.temporal.common.RetryOptions
import io.temporal.workflow.Workflow
import mu.KotlinLogging
import java.time.Duration

/**
 * The implementation class for notifying if an upload has not occurred within a specified time
 * @property activities T
 */
class NotificationWorkflowImpl : NotificationWorkflow {
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
    * The function which gets invoked by the temporal WF engine which checks whether upload has occurred within a specified time or not
    * invokes the activity, if there are errors
    * @param dataStreamId String
    * @param dataStreamRoute String
    * @param jurisdiction String
    * @param daysToRun List<String>
    * @param timeToRun String
    * @param deliveryReference String
    */
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
            logger.error("Error occurred while checking for upload deadline: ${e.message}")
        }

    }
    /**
     *  The actual function which checks for whether the upload has occurred or not within a specified time
     *   @param dataStreamId String
     *   @param jurisdiction String
     */
    private fun checkUpload(dataStreamId: String, jurisdiction: String): Boolean {
        // add check logic here
        return false
    }


}