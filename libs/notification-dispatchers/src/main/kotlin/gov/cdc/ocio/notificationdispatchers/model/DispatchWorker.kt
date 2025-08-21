package gov.cdc.ocio.notificationdispatchers.model

/**
 * Interface for dispatch workers.
 */
fun interface DispatchWorker {

    /**
     * Sends a notification with the content provided.
     *
     * @param content NotificationContent
     */
    fun send(content: NotificationContent)
}
