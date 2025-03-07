package gov.cdc.ocio.processingnotifications.workflow

import gov.cdc.ocio.database.persistence.ProcessingStatusRepository
import gov.cdc.ocio.processingnotifications.activity.NotificationActivities
import gov.cdc.ocio.processingnotifications.model.CheckUploadResponse
import gov.cdc.ocio.processingnotifications.utils.SqlClauseBuilder
import io.temporal.activity.ActivityOptions
import io.temporal.common.RetryOptions
import io.temporal.workflow.Workflow
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter


/**
 * The implementation class for upload errors.
 *
 * @property activities T
 */
class UploadErrorsNotificationWorkflowImpl : UploadErrorsNotificationWorkflow, KoinComponent {

    private val logger = KotlinLogging.logger {}

    private val repository by inject<ProcessingStatusRepository>()

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
            val uploadIdsWithErrors = checkUploadErrors(dataStreamId, dataStreamRoute, jurisdiction)
            if (uploadIdsWithErrors.isNotEmpty()) {
                activities.sendUploadErrorsNotification(uploadIdsWithErrors, deliveryReference)
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
    private fun checkUploadErrors(
        dataStreamId: String,
        dataStreamRoute: String,
        jurisdiction: String
    ): List<CheckUploadResponse> {

        // Get today's date in UTC
        val today = LocalDate.now(ZoneId.of("UTC"))
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
        val reportsCollection = repository.reportsCollection
        val collectionName = reportsCollection.collectionNameForQuery
        val cVar = reportsCollection.collectionVariable
        val cPrefix = reportsCollection.collectionVariablePrefix

        val dateStart = today.atStartOfDay(ZoneOffset.UTC).format(formatter)
        val dateEnd = today.atTime(23, 59, 59).atZone(ZoneOffset.UTC).format(formatter)
        val timeRangeWhereClause = SqlClauseBuilder().buildSqlClauseForDateRange(null, dateStart, dateEnd, cPrefix)

        val notificationQuery = """
           SELECT ${cPrefix}id 
           FROM $collectionName $cVar 
           WHERE ${cPrefix}dataStreamId = '$dataStreamId'
               AND ${cPrefix}dataStreamRoute = '$dataStreamRoute'
               AND ${cPrefix}jurisdiction = '$jurisdiction'
               AND ${cPrefix}status = 'failure'
               AND $timeRangeWhereClause 
           """.trimIndent()

        logger.info("notification Query: $notificationQuery")

        return try {
            val results = repository.reportsCollection.queryItems(
                notificationQuery,
                CheckUploadResponse::class.java
            )
            logger.info("notification Query results: ${results.size}")
            results
        } catch (ex: Exception) {
            logger.error("Error occurred while checking upload for $dataStreamId and $jurisdiction: ${ex.message}")
            throw Exception("Error occurred in checking upload")
        }
    }
}
