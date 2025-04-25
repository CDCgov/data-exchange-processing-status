package gov.cdc.ocio.notificationdispatchers.logger

import gov.cdc.ocio.notificationdispatchers.model.DispatchWorker
import gov.cdc.ocio.notificationdispatchers.model.NotificationContent
import mu.KotlinLogging


/**
 * Simply logs the notification event rather than trying to send it anywhere.
 *
 * @property logger KLogger
 */
class LoggerDispatchWorker: DispatchWorker {
    private val logger = KotlinLogging.logger {}

    /**
     * Sends a notification with the content provided.
     *
     * @param content NotificationContent
     */
    override fun send(content: NotificationContent) {
        logger.info { "Notification content:\n$content" }
    }
}