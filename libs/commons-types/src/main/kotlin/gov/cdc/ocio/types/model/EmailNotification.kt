package gov.cdc.ocio.types.model


data class EmailNotification(
    val emailAddresses: Collection<String>
) : Notification(NotificationType.EMAIL)