package gov.cdc.ocio.notificationdispatchers.model


/**
 * The content for logging a notification can be [Any].
 *
 * @property content Any
 * @constructor
 */
data class LoggerNotificationContent(
    val content: Any
): NotificationContent()