package gov.cdc.ocio.processingnotifications.activity

import io.temporal.activity.ActivityInterface
import io.temporal.activity.ActivityMethod

@ActivityInterface
interface NotificationActivities {
    @ActivityMethod
    fun sendNotification(
        dataStreamId: String,
        dataStreamRoute: String,
        jurisdiction: String,
        deliveryReference: String
    )
}
