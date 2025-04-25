package gov.cdc.ocio.types.model


/**
 * Parameters for the email notification.
 *
 * @property emailAddresses List<String>
 * @constructor
 */
data class EmailNotification(
    val emailAddresses: List<String>
) : Notification(NotificationType.EMAIL)