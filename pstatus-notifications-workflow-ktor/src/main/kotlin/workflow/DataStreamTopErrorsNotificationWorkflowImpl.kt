package gov.cdc.ocio.processingnotifications.workflow

import gov.cdc.ocio.processingnotifications.activity.NotificationActivities
import gov.cdc.ocio.processingnotifications.model.ErrorDetail
import io.temporal.activity.ActivityOptions
import io.temporal.common.RetryOptions
import io.temporal.workflow.Workflow
import mu.KotlinLogging
import java.time.Duration


/**
 * The implementation class which determines the digest counts and top errors during an upload and its frequency.
 *
 * @property logger KLogger
 * @property activities (NotificationActivities..NotificationActivities?)
 * @property errorList List<String>
 */
class DataStreamTopErrorsNotificationWorkflowImpl
    : DataStreamTopErrorsNotificationWorkflow {

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

    //TODO : This should come  from db in real application
    val errorList = listOf(
        "DataStreamId missing",
        "DataStreamRoute missing",
        "Jurisdiction missing",
        "DataStreamId missing",
        "Jurisdiction missing",
        "DataStreamRoute missing",
        "DataStreamRoute missing",
        "DataStreamId missing",
        "DataStreamId missing",
        "DataStreamRoute missing",
        "DataStreamId missing",
        "DataStreamId missing",
    )

    /**
     * The function which determines the digest counts and top errors during an upload and its frequency.
     *
     * @param dataStreamId String
     * @param dataStreamRoute String
     * @param jurisdiction String
     * @param cronSchedule String
     * @param emailAddresses List<String>
     */
    override fun checkDataStreamTopErrorsAndNotify(
        dataStreamId: String,
        dataStreamRoute: String,
        jurisdiction: String,
        cronSchedule: String,
        emailAddresses: List<String>
    ) {
        try {
            // Logic to check if the upload occurred*/
            val (totalCount, topErrors)  = getTopErrors(errorList)
            val errors = topErrors.filter { it.description.isNotEmpty() }.joinToString()
            if (topErrors.isNotEmpty()) {
                activities.sendDataStreamTopErrorsNotification("There are $totalCount errors \n These are the top errors : \n $errors \n", emailAddresses)
            }
        } catch (e: Exception) {
            logger.error("Error occurred while checking for counts and top errors and frequency in an upload: ${e.message}")
        }
    }

    /**
     * Function which actually does find the counts and the erroneous fields and its frequency.
     *
     * @param errors List<String>
     * @return Pair<int, List<ErrorDetail>
     */
    private fun getTopErrors(errors: List<String>): Pair<Int, List<ErrorDetail>> {
        // Group the errors by description and count their occurrences
        val errorCounts = errors.groupingBy { it }.eachCount()

        // Convert the grouped data into a list of ErrorDetail objects
        val errorDetails = errorCounts.map { (description, count) ->
            ErrorDetail(description, count)
        }
        // Sort the errors by their count in descending order and take the top 5
        val topErrors = errorDetails.sortedByDescending { it.count }.take(5)

        // Return the total count of errors and the top 5 errors
        return Pair(errors.size, topErrors)
    }

}
