package gov.cdc.ocio.processingstatusnotifications.model

abstract class Notification(
    val notificationType: SubscriptionType
): NotificationAction