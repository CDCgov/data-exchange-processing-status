package gov.cdc.ocio.processingnotifications.activity

import gov.cdc.ocio.notificationdispatchers.NotificationDispatcher
import gov.cdc.ocio.notificationdispatchers.model.EmailNotificationContent
import gov.cdc.ocio.notificationdispatchers.model.WebhookNotificationContent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


/**
 * Implementation class for sending email notifications for various notifications
 */
abstract class NotificationActivitiesImpl : NotificationActivities, KoinComponent {

    private val notifications by inject<NotificationDispatcher>()

    private val fromEmail = "donotreply@cdc.gov"
    private val fromName = "Do not reply (PHDO team)"

    override fun sendEmail(emailAddresses: List<String>, subject: String, body: String) {
        notifications.send(
            EmailNotificationContent(
                emailAddresses,
                fromEmail,
                fromName,
                subject,
                body
            )
        )
    }

    override fun sendWebhook(url: String, body: Any) {
        notifications.send(
            WebhookNotificationContent(
                url, body
            )
        )
    }
}
