package gov.cdc.ocio.processingnotifications.email

import mu.KotlinLogging
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Socket
import javax.mail.internet.MimeMessage
import java.util.*
import javax.mail.Message
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress


/**
 * The class which dispatches the email using SMTP
 */
class EmailDispatcher {
    private val logger = KotlinLogging.logger {}

    /**
     * Method to send email which checks the SMTP status and then invokes sendEmail.
     *
     * @param subject String
     * @param body String
     * @param toEmailAddresses List<String>
     */
    fun sendEmail(
        subject: String,
        body: String,
        toEmailAddresses: List<String>
    ) {
        try {
            if (!checkSMTPStatusWithoutCredentials()) return
            // TODO : Change this into properties
            val toEmailAddressesStr = toEmailAddresses.joinToString(",")
            val props = System.getProperties()
            props["mail.smtp.host"] = "smtpgw.cdc.gov"
            props["mail.smtp.port"] = 25
            val session = Session.getInstance(props, null)
            sendEmail(session, toEmailAddressesStr, subject, body)
        } catch(e: Exception) {
            logger.error("Unable to send email ${e.message}")
        }
    }

    /**
     * Method to send email.
     *
     * @param session Session
     * @param toEmail String
     * @param subject String
     * @param body String
     */
    private fun sendEmail(session: Session?, toEmail: String?, subject: String?, body: String?) {
        try {
            val msg = MimeMessage(session)
            val replyToEmail = "donotreply@cdc.gov"
            val replyToName = "DoNotReply (PHDO Team)"
            //set message headers
            msg.addHeader("Content-type", "text/HTML; charset=UTF-8")
            msg.addHeader("format", "flowed")
            msg.addHeader("Content-Transfer-Encoding", "8bit")

            //TODO - Change the from and replyTo address after the new licensed account is created
            // Get the email addresses from the property
            msg.setFrom(InternetAddress(replyToEmail, replyToName))
            msg.replyTo = InternetAddress.parse(replyToEmail, false)
            msg.setSubject(subject, "UTF-8")
            msg.setText(body, "UTF-8", "html")
            msg.sentDate = Date()
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail, false))
            Transport.send(msg)
        } catch (e: Exception) {
            logger.error("Unable to send email ${e.message}")
        }
    }

    /**
     * Method to check the status of the SMTP server
     */
    private fun checkSMTPStatusWithoutCredentials(): Boolean {
        // This is to get the status from curl statement to see if server is connected
        try {
            val smtpServer = "smtpgw.cdc.gov"
            val port = 25
            val socket = Socket(smtpServer, port)
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            // Read the server response
            val response = reader.readLine()
            println("Server response: $response")
            // Close the socket
            socket.close()
            return response !=null
        } catch (e: Exception) {
            logger.error("Unable to send email. Error is ${e.message} \n. Stack trace : ${e.printStackTrace()}")
        }
        return false
    }
}