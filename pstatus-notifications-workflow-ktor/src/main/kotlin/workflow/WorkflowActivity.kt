package gov.cdc.ocio.processingnotifications.workflow

import gov.cdc.ocio.processingnotifications.activity.NotificationActivities
import io.temporal.activity.ActivityOptions
import io.temporal.common.RetryOptions
import io.temporal.workflow.Workflow
import java.time.Duration

object WorkflowActivity {

    /**
     * Creates and returns a stub for the `NotificationActivities` interface configured with the specified activity
     * options. The stub allows workflow implementations to invoke activity methods with appropriate timeout settings
     * and retry policies.
     *
     * @return An instance of `NotificationActivities` that can be used to execute activity methods as part of a workflow.
     */
    fun newDefaultActivityStub(): NotificationActivities = Workflow.newActivityStub(
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

}