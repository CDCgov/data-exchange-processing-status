package gov.cdc.ocio.processingstatusnotifications.model

/**
 * Functional interface for the notification actions.
 */
fun interface NotificationAction {

    /**
     * Common interface for sending a notification with the provided content.
     *
     * @param content Any
     */
    fun doNotify(content: Any)
}