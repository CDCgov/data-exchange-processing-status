import io.temporal.activity.ActivityOptions
import io.temporal.common.RetryOptions
import io.temporal.workflow.Workflow
import java.time.Duration
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter


class NotificationWorkflowImpl : NotificationWorkflow {

    private val activities = Workflow.newActivityStub(
        NotificationActivities::class.java,
        ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(10)) // Set the start-to-close timeout
            .setScheduleToCloseTimeout(Duration.ofMinutes(1)) // Set the schedule-to-close timeout
            .setRetryOptions(
                RetryOptions.newBuilder()
                .setMaximumAttempts(3) // Set retry options if needed
                .build())
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
        val formatter = DateTimeFormatter.ofPattern("HH:mm:ssXXX")
        val timeToRunParsed = LocalTime.parse(timeToRun, formatter)
        val now = LocalTime.now(ZoneOffset.UTC)
        val dayOfWeek= ZonedDateTime.now(ZoneOffset.UTC).dayOfWeek.toString().substring(0, 2).capitalizeWords()


        val waitTime = if (timeToRunParsed.isAfter(now)) {
            Duration.between(now, timeToRunParsed)
        } else {
            Duration.between(now, timeToRunParsed.plusHours(24))
        }
        //if (daysToRun.contains(dayOfWeek.name.substring(0, 2))) {
        if (daysToRun.contains(dayOfWeek)) {
            Workflow.sleep(waitTime)
            // Logic to check if the upload occurred
            val uploadOccurred = checkUpload(dataStreamId, jurisdiction)

            if (!uploadOccurred) {
                activities.sendNotification(dataStreamId, dataStreamRoute, jurisdiction, deliveryReference)
            }
        }

        // Re-run the workflow the next day
        Workflow.sleep(Duration.ofHours(24))
    }

    private fun checkUpload(dataStreamId: String, jurisdiction: String): Boolean {
        // Simulated check logic
        return false
    }

    private fun String.capitalizeWords(delimiter: String = " ") =
        split(delimiter).joinToString(delimiter) { word ->

            val smallCaseWord = word.lowercase()
            smallCaseWord.replaceFirstChar(Char::titlecaseChar)

        }
}
