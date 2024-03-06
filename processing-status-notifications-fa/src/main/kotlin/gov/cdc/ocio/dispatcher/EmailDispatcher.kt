package gov.cdc.ocio.dispatcher

import gov.cdc.ocio.dispatcher.EmailUtil.sendEmail
import gov.cdc.ocio.model.cache.NotificationSubscription
import mu.KotlinLogging
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Socket
import javax.mail.MessagingException
import javax.mail.Session

/**
 * EMail dispatcher implements IDispatcher which will implement code to send out emails
 * to the subscribers of teh rules (if the rule matches in rule engine evaluation phase)
 * @property logger KLogger
 */
class EmailDispatcher(): IDispatcher {
    private val logger = KotlinLogging.logger {}
    override fun dispatchEvent(subscription: NotificationSubscription): String {

        // Call the function to check SMTP status
        var response = checkSMTPStatusWithoutCredentials(subscription)
        if (response.contains("Service ready", true)) {
            logger.info("Email service connected")
            sendEmail(subscription)
            return "Email sent successfully"
        } else {
            return "Email server not reachable"
        }

    }

    /**
     * Method to check teh status of the SMTP server
     * @param subscription NotificationSubscription
     * @return String
     */
    private fun checkSMTPStatusWithoutCredentials(subscription: NotificationSubscription): String {
        // This is to get the status from curl statement to see if server is connected
        try {
            val smtpServer = System.getenv("SmtpHostServer")
            val port = System.getenv("SmtpHostPort").toInt()
            val socket = Socket(smtpServer, port)
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            // Read the server response
            val response = reader.readLine()
            println("Server response: $response")
            // Close the socket
            socket.close()
            return response
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }

    /**
     * Method to send email
     * @param subscription NotificationSubscription
     * @return String
     */
    private fun sendEmail(subscription: NotificationSubscription): Unit {

        try{
            // TODO : Change this into properties
            val toEmalId = subscription.subscriberAddressOrUrl;
            val props = System.getProperties()
            props["mail.smtp.host"] = System.getenv("SmtpHostServer")
            props["mail.smtp.port"] = System.getenv("SmtpHostPort")
            val session = Session.getInstance(props, null)
            sendEmail(session, toEmalId, "${System.getenv("ReplyToName")} : Notification for report", "You have signed up for this notification subscription, please check the portal")
        } catch(_: MessagingException) {
            logger.info("Unable to send email")
        }

    }

}