package gov.cdc.ocio.processingnotifications.activity

import gov.cdc.ocio.processingnotifications.dispatch.Dispatcher
import gov.cdc.ocio.types.notification.Notifiable
import io.temporal.activity.ActivityInterface
import io.temporal.activity.ActivityMethod


/**
 * Interface which defines the activity methods
 */
@ActivityInterface
interface NotificationActivities {
//    @ActivityMethod
//    fun sendNotification(
//        dataStreamId: String,
//        jurisdiction: String,
//        emailAddresses: List<String>
//    )
//    @ActivityMethod
//    fun sendDataStreamTopErrorsNotification(
//        emailBody: String,
//        emailAddresses: List<String>
//    )

//    @ActivityMethod
//    fun sendDigestEmail(
//        emailBody: String,
//        emailAddresses: List<String>)

    @ActivityMethod
    fun dispatchNotification(payload: Notifiable, dispatcher: Dispatcher)
}
