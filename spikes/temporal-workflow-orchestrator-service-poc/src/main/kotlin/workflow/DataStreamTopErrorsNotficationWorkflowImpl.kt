package gov.cdc.ocio.processingnotifications.workflow

import gov.cdc.ocio.processingnotifications.activity.NotificationActivities
import gov.cdc.ocio.processingnotifications.model.ErrorDetail
import io.temporal.activity.ActivityOptions
import io.temporal.common.RetryOptions
import io.temporal.workflow.Workflow
import java.time.Duration

class DataStreamTopErrorsNotficationWorkflowImpl : DataStreamTopErrorsNotificationWorkflow {
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
    override fun checkDataStreamTopErrorsAndNotify(
        dataStreamId: String,
        dataStreamRoute: String,
        jurisdiction: String,
        daysToRun: List<String>,
        timeToRun: String,
        deliveryReference: String
    ) {
        try {
            // Logic to check if the upload occurred*/
            val (totalCount, topErrors)  = getTopErrors(errorList)
            val errors = topErrors.filter{it.description.isNotEmpty()}.joinToString()
            if (topErrors.isNotEmpty()) {
                activities.sendUploadErrorsNotification("There are $totalCount errors and these are the topErrors :$errors",deliveryReference)
            }
        } catch (e: Exception) {
        }
    }


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
