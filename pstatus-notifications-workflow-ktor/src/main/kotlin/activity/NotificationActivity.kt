package gov.cdc.ocio.processingnotifications.activity

import io.temporal.activity.ActivityInterface
import io.temporal.activity.ActivityMethod


/**
 * Interface which defines the activity methods
 */
@ActivityInterface
interface NotificationActivities {

    @ActivityMethod
    fun sendEmail(emailAddresses: List<String>, subject: String, body: String)

    @ActivityMethod
    fun sendWebhook(url: String, body: Any)
}
