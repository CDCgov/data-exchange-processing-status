package gov.cdc.ocio.processingnotifications.activity

import gov.cdc.ocio.notificationdispatchers.NotificationDispatcher
import gov.cdc.ocio.notificationdispatchers.model.EmailNotificationContent
import gov.cdc.ocio.notificationdispatchers.model.WebhookNotificationContent
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDate


/**
 * Implementation class for sending email notifications for various notifications
 */
class NotificationActivitiesImpl : NotificationActivities, KoinComponent {

    private val logger = KotlinLogging.logger {}

    private val notifications by inject<NotificationDispatcher>()

    private val fromEmail = "donotreply@cdc.gov"
    private val fromName = "Do not reply (PHDO team)"

    /**
     * Send notification method which uses the email service to send email when an upload fails
     * @param dataStreamId String
     * @param jurisdiction String
     * @param emailAddresses List<String>
     */
    override fun sendNotification(
        dataStreamId: String,
        jurisdiction: String,
        emailAddresses: List<String>
    ) {
        val msg = ("Upload deadline over. Failed to get the upload for dataStreamId: $dataStreamId, "
                + "jurisdiction: $jurisdiction on " + LocalDate.now() + ".")

        logger.info(msg)
        notifications.send(
            EmailNotificationContent(
                emailAddresses,
                fromEmail,
                fromName,
                "UPLOAD DEADLINE CHECK EXPIRED for $jurisdiction on " + LocalDate.now(),
                msg
            )
        )
    }

    /**
     * Send notification method which uses the email service to send email with the digest counts of the top errors in
     * an upload.
     *
     * @param emailBody String
     * @param emailAddresses List<String>
     */
    override fun sendDataStreamTopErrorsNotification(
        emailBody: String,
        emailAddresses: List<String>
    ) {
        logger.info(emailBody)
        notifications.send(
            EmailNotificationContent(
                emailAddresses,
                fromEmail,
                fromName,
                "DATA STREAM TOP ERRORS NOTIFICATION",
                emailBody
            )
        )
    }

    /**
     * Sends an email with the daily upload digest counts.
     *
     * @param emailBody String
     * @param emailAddresses List<String>
     */
    override fun sendDigestEmail(
        emailBody: String,
        emailAddresses: List<String>
    ) {
        notifications.send(
            EmailNotificationContent(
                emailAddresses,
                fromEmail,
                fromName,
                "PHDO UPLOAD DIGEST NOTIFICATION",
                emailBody
            )
        )
    }

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
