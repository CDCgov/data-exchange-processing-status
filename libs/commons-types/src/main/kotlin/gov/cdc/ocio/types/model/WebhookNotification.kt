package gov.cdc.ocio.types.model


/**
 * Parameters for the webhook notification.
 *
 * @property webhookUrl String
 * @constructor
 */
data class WebhookNotification(
    val webhookUrl: String
) : Notification(NotificationType.WEBHOOK)