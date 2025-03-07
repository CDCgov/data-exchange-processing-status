package gov.cdc.ocio.processingnotifications.activity

import gov.cdc.ocio.processingnotifications.model.CheckUploadResponse
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
        jurisdiction: String,
        deliveryReference: String
    )
    @ActivityMethod
    fun sendUploadErrorsNotification(
        error: List<CheckUploadResponse>,
        deliveryReference: String
    )
    @ActivityMethod
    fun sendDataStreamTopErrorsNotification(
        error:String,
        deliveryReference: String
    )

    @ActivityMethod
    fun sendDigestEmail(
        emailBody: String,
        deliveryReference: String)
}
