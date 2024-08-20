import io.temporal.activity.ActivityOptions
import io.temporal.workflow.Workflow
import java.time.*
import java.time.format.DateTimeFormatter

class DeadlineCheckWorkflowImpl : DeadlineCheckWorkflow {
    // Define ActivityOptions with timeouts
    private val notificationActivity = Workflow.newActivityStub(
        NotificationActivities::class.java,
        ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofMinutes(5)) // Maximum time allowed for the activity to complete
            .setScheduleToCloseTimeout(Duration.ofMinutes(8)) // Total time allowed from scheduling to completion
            .build()
    )

    override fun execute(
        jurisdiction: String,
        dataStreamId: String,
        dataStreamRoute: String,
        daysToRun: List<String>,
        timeToRun: String,
        deliveryReference: String
    ) {
        val parsedTime = LocalTime.parse(timeToRun.substring(0, 8), DateTimeFormatter.ofPattern("HH:mm:ss"))
        val zoneOffset = ZoneId.of(timeToRun.substring(8))

        while (true) {
            val now = ZonedDateTime.now(zoneOffset)
            val runTime = now.with(parsedTime)

            val delay = Duration.between(now, runTime).toMillis()

            if (daysToRun.contains(now.dayOfWeek.name.substring(0, 2))) {
                Workflow.sleep(delay)

                // After delay, check if the upload occurred. If not, send notification.
                val uploadOccurred = checkUpload(jurisdiction, dataStreamId, dataStreamRoute)
                if (!uploadOccurred) {
                  //  notificationActivity.sendNotification(jurisdiction, dataStreamId, deliveryReference)
                }
            }
            // Wait for the next day to run
            Workflow.sleep(Duration.ofDays(1).toMillis() - delay)
        }

    }

    private fun checkUpload(jurisdiction: String, dataStreamId: String, dataStreamRoute: String): Boolean {
        // Add logic to check if an upload occurred
        return false
    }
}
