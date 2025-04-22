package gov.cdc.ocio.notificationdispatchers.model


/**
 * Model needed by the notification dispatcher for invoking webhooks.
 *
 * @property webhookUrl String
 * @property content Any
 * @constructor
 */
data class WebhookNotificationContent(
    val webhookUrl: String,
    val content: Any
): NotificationContent()