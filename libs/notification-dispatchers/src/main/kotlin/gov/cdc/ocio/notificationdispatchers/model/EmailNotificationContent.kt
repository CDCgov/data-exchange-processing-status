package gov.cdc.ocio.notificationdispatchers.model


/**
 * Model needed by the notification dispatcher for sending emails.
 *
 * @property to List<String>
 * @property fromEmail String
 * @property fromName String
 * @property subject String
 * @property body String
 * @constructor
 */
data class EmailNotificationContent(
    val to: List<String>,
    val fromEmail: String,
    val fromName: String,
    val subject: String,
    val body: String
): NotificationContent()