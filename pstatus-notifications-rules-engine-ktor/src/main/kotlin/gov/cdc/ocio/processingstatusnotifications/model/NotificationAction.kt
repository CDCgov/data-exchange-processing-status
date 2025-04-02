package gov.cdc.ocio.processingstatusnotifications.model

/**
 * Functional interface for the notification actions.
 */
fun interface NotificationAction {

    /**
     * Common interface for sending a notification with the provided payload.
     *
     * @param payload Any
     */
    fun doNotify(payload: Any)
}