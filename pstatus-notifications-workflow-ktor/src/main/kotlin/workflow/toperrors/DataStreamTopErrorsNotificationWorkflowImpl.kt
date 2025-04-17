package gov.cdc.ocio.processingnotifications.workflow.toperrors

import gov.cdc.ocio.processingnotifications.activity.NotificationActivities
import gov.cdc.ocio.processingnotifications.model.ErrorDetail
import gov.cdc.ocio.processingnotifications.model.WorkflowSubscription
import gov.cdc.ocio.processingnotifications.service.ReportService
import io.temporal.activity.ActivityOptions
import io.temporal.common.RetryOptions
import io.temporal.workflow.Workflow
import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import mu.KotlinLogging
import java.time.Duration


/**
 * The implementation class which determines the digest counts and top errors during an upload and its frequency.
 *
 * @property logger KLogger
 * @property reportService ReportService
 * @property activities (NotificationActivities..NotificationActivities?)
 */
class DataStreamTopErrorsNotificationWorkflowImpl
    : DataStreamTopErrorsNotificationWorkflow {

    private val logger = KotlinLogging.logger {}
    private val reportService = ReportService()

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
     * The function which determines the digest counts and top errors during an upload and its frequency.
     *
     * @param dataStreamId String
     * @param dataStreamRoute String
     * @param jurisdiction String
     * @param cronSchedule String
     * @param emailAddresses List<String>
     */
    override fun checkDataStreamTopErrorsAndNotify(
        workflowSubscription: WorkflowSubscription
    ) {
        val dayInterval = workflowSubscription.daysInterval ?: 5 // TODO make this default value configurable
        val dataStreamId = workflowSubscription.dataStreamId
        val dataStreamRoute = workflowSubscription.dataStreamRoute
        try {
            // Logic to check if the upload occurred*/
            val failedMetadataVerifyCount = reportService.countFailedReports(dataStreamId, dataStreamRoute, "metadata-verify", dayInterval)
            val failedDeliveryCount = reportService.countFailedReports(dataStreamId, dataStreamRoute, "blob-file-copy", dayInterval)
            val delayedUploads = reportService.getDelayedUploads(dataStreamId, dataStreamRoute, dayInterval)
            val delayedDeliveries = reportService.getDelayedDeliveries(dataStreamId, dataStreamRoute, dayInterval)
            val body = formatEmailBody(
                dataStreamId,
                dataStreamRoute,
                failedMetadataVerifyCount,
                failedDeliveryCount,
                delayedUploads,
                delayedDeliveries,
                dayInterval
            )
            workflowSubscription.emailAddresses?.let { activities.sendDataStreamTopErrorsNotification(body, workflowSubscription.emailAddresses) }
        } catch (e: Exception) {
            logger.error("Error occurred while checking for counts and top errors and frequency in an upload: ${e.message}")
        }
    }

    /**
     * Builds the HTML string of the email body
     *
     * @param dataStreamId String
     * @param dataStreamRoute String
     * @param failedMetadataValidationCount Int
     * @param failedDeliveryCount Int
     * @param delayedUploads List<String>
     * @param delayedDeliveries List<String>
     * @param daysInterval Int
     */
    private fun formatEmailBody(
        dataStreamId: String,
        dataStreamRoute: String,
        failedMetadataValidationCount: Int,
        failedDeliveryCount: Int,
        delayedUploads: List<String>,
        delayedDeliveries: List<String>,
        daysInterval: Int
    ): String {
        return buildString {
            appendHTML().html {
                body {
                    h2 { +"$dataStreamId $dataStreamRoute Upload Issues in the last $daysInterval days" }
                    br {  }
                    h3 { +"Total: ${failedMetadataValidationCount + failedDeliveryCount + delayedUploads.size + delayedDeliveries.size }" }
                    ul {
                        li { +"Failed Metadata Validation: $failedMetadataValidationCount" }
                        li { +"Failed Deliveries: $failedDeliveryCount" }
                        li { +"Delayed Uploads: ${delayedUploads.size}" }
                        li { +"Delayed Deliveries: ${delayedDeliveries.size}" }
                    }
                    br {  }
                    h3 { +"Delayed Uploads" }
                    ul {
                        delayedUploads.map { li { +it } }
                    }
                    br {  }
                    h3 { +"Delayed Deliveries" }
                    ul {
                        delayedDeliveries.map{ li { +it }}
                    }
                }
            }
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
