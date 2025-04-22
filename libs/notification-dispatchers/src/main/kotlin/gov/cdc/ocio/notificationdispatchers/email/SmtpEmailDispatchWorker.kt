package gov.cdc.ocio.notificationdispatchers.email

import gov.cdc.ocio.notificationdispatchers.model.DispatchWorker
import gov.cdc.ocio.notificationdispatchers.model.EmailNotificationContent
import gov.cdc.ocio.notificationdispatchers.model.NotificationContent
import gov.cdc.ocio.notificationdispatchers.model.SmtpConfig
import jakarta.mail.*
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import mu.KotlinLogging
import java.util.*


class SmtpEmailDispatchWorker(private val config: SmtpConfig): DispatchWorker {

    private val logger = KotlinLogging.logger {}

    private fun createSession(): Session {
        val props = Properties().apply {
            put("mail.smtp.auth", config.auth.toString())
            put("mail.smtp.starttls.enable", config.enableTls.toString())
            put("mail.smtp.host", config.host)
            put("mail.smtp.port", config.port.toString())
        }

        return Session.getInstance(
            props,
            if (config.auth) object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(config.username, config.password)
                }
            } else null
        )
    }

    /**
     * Sends an email with the SMTP configuration provided.
     *
     * @param to List<String> list of recipient email addresses
     * @param fromEmail String
     * @param fromName String
     * @param subject String
     * @param body String
     */
    private fun send(to: List<String>, fromEmail: String, fromName: String, subject: String, body: String) {
        try {
            val session = createSession()
            val toEmailAddresses = InternetAddress.parse(to.joinToString(","))
            val message = MimeMessage(session).apply {
                addHeader("Content-type", "text/HTML; charset=UTF-8")
                addHeader("format", "flowed")
                addHeader("Content-Transfer-Encoding", "8bit")

                setFrom(InternetAddress(fromEmail, fromName))
                setRecipients(Message.RecipientType.TO, toEmailAddresses)
                setSubject(subject, "UTF-8")
                sentDate = Date()
                setText(body, "UTF-8", "html")
            }
            logger.info("Sending email to: $to")
            Transport.send(message)
        } catch (e: Exception) {
            logger.error { "Failed to send email to, '$to' with exception: ${e.localizedMessage}" }
        }
    }

    override fun send(content: NotificationContent) {
        if (content !is EmailNotificationContent) error("content must be EmailNotificationContent")

        send(content.to, content.fromEmail, content.fromName, content.subject, content.body)
    }
}