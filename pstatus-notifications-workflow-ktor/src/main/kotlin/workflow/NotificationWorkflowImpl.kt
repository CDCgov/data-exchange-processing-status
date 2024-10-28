package gov.cdc.ocio.processingnotifications.workflow

import gov.cdc.ocio.database.persistence.ProcessingStatusRepository
import gov.cdc.ocio.processingnotifications.activity.NotificationActivities
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
 * The implementation class for notifying if an upload has not occurred within a specified time
 * @property activities T
 */
class NotificationWorkflowImpl : NotificationWorkflow, KoinComponent {
    private val cosmosRepository by inject<ProcessingStatusRepository>()
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
    * @param jurisdiction String
    * @param daysToRun List<String>
    * @param timeToRun String
    * @param deliveryReference String
    */
    override fun checkUploadAndNotify(
        dataStreamId: String,
        jurisdiction: String,
        daysToRun: List<String>,
        timeToRun: String,
        deliveryReference: String
    ) {

        try {
            // Logic to check if the upload occurred*/
            val uploadOccurred = checkUpload(dataStreamId, jurisdiction)
            if (!uploadOccurred) {
                activities.sendNotification(dataStreamId, jurisdiction, deliveryReference)
            }
        } catch (e: Exception) {
            logger.error("Error occurred while checking for upload deadline: ${e.message}")
            throw Exception("Error occurred while checking for upload deadline")
        }


    }

    /**
     *  The actual function which checks for whether the upload has occurred or not within a specified time
     *   @param dataStreamId String
     *   @param jurisdiction String
     *  @return True if there are records; false otherwise.
     *  @throws Exception if an error occurs while querying the db.
     */
    private fun checkUpload(dataStreamId: String, jurisdiction: String): Boolean {
        /** Get today's date in UTC **/
        val today = LocalDate.now(ZoneId.of("UTC"))
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")

        val dateStart = today.atStartOfDay(ZoneOffset.UTC).format(formatter)
        val dateEnd = today.atTime(12, 0, 0).atZone(ZoneOffset.UTC).format(formatter)
        val timeRangeWhereClause = SqlClauseBuilder().buildSqlClauseForDateRange(null, dateStart, dateEnd)

        val reportsCollection =cosmosRepository.reportsCollection
        val cVar = reportsCollection.collectionVariable
        val cPrefix =reportsCollection.collectionVariablePrefix
        val notificationQuery = (
                "SELECT VALUE COUNT(1) "
                        + "from $cVar where  ${cPrefix}dataStreamId = '$dataStreamId' and "
                        + "${cPrefix}jurisdiction = '$jurisdiction' and ($timeRangeWhereClause)"
                )
        logger.info("notification Query: $notificationQuery")

        return try {
            val results = cosmosRepository.reportsCollection.queryItems(
                notificationQuery,
                Integer::class.java
            )
            val count = results.size
            count != 0 // Returns false if count == 0
        } catch (ex: Exception) {
            logger.error("Error occurred while checking upload for $dataStreamId and $jurisdiction: ${ex.message}")
            throw Exception("Error occurred in checking upload")
        }
    }


}
