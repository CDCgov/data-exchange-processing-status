package gov.cdc.ocio.dispatcher

import java.util.*
import javax.mail.Message
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

object EmailUtil {
    /**
     * Utility method to send simple HTML email
     * @param session
     * @param toEmail
     * @param subject
     * @param body
     */
    fun sendEmail(session: Session?, toEmail: String?, subject: String?, body: String?) {
        try {
            val msg = MimeMessage(session)
            val replyToEmail = System.getenv("ReplyToEmail")
            val replyToName = System.getenv("ReplyToName")
            //set message headers
            msg.addHeader("Content-type", "text/HTML; charset=UTF-8")
            msg.addHeader("format", "flowed")
            msg.addHeader("Content-Transfer-Encoding", "8bit")

            //TODO - Change the from and replyTo address after the new licensed account is created
            // Get the email addresses from the property
            msg.setFrom(InternetAddress(replyToEmail, replyToName))
            msg.replyTo = InternetAddress.parse(replyToEmail, false)
            msg.setSubject(subject, "UTF-8")
            msg.setText(body, "UTF-8")
            msg.sentDate = Date()
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail, false))
            Transport.send(msg)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}