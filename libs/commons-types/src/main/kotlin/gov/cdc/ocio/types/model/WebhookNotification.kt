package gov.cdc.ocio.types.model


data class WebhookNotification(
    val webhookUrl: String
) : Notification(NotificationType.WEBHOOK)