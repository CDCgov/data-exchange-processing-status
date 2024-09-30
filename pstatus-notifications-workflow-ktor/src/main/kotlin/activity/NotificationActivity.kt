package gov.cdc.ocio.processingnotifications.activity

import io.temporal.activity.ActivityInterface
import io.temporal.activity.ActivityMethod

/**
 * Interface which defines the activity methods
 */
@ActivityInterface
interface NotificationActivities {
    @ActivityMethod
    fun sendNotification(
        dataStreamId: String,
        //dataStreamRoute: String,
        jurisdiction: String,
        deliveryReference: String
    )
    @ActivityMethod
    fun sendUploadErrorsNotification(
        error:String,
        deliveryReference: String
    )
    @ActivityMethod
    fun sendDataStreamTopErrorsNotification(
        error:String,
        deliveryReference: String
    )
}
