package gov.cdc.ocio.notificationdispatchers.model


/**
 * Model needed by the notification dispatcher for invoking webhooks.
 *
 * @property webhookUrl String
 * @property payload Any
 * @constructor
 */
data class WebhookNotificationContent(
    val webhookUrl: String,
    val payload: Any
): NotificationContent()