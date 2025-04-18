package gov.cdc.ocio.notificationdispatchers.email

import mu.KotlinLogging

/**
 * Logs
 * @property logger KLogger
 */
class LoggingEmailDispatcher: EmailDispatcher {
    private val logger = KotlinLogging.logger {}

    override fun send(to: List<String>, fromEmail: String, fromName: String, subject: String, body: String) {
        logger.info { "Email not sent!  Only logged." }
    }
}