package gov.cdc.ocio.processingstatusnotifications.model

import gov.cdc.ocio.processingstatusnotifications.dispatcher.EmailUtil
import gov.cdc.ocio.processingstatusnotifications.exception.BadRequestException
import javax.mail.Session

class EmailNotification(
    private val emailAddresses: Collection<String>
) : Notification(SubscriptionType.EMAIL) {

    /**
     * For emails, the payload should be a string containing HTML.
     * @param payload Any
     */
    override fun doNotify(payload: Any) {
        if (payload !is EmailPayload) throw BadRequestException("Email payload is not in the expected format")

        val toEmail = emailAddresses.joinToString(",")
        val props = System.getProperties()
        props["mail.smtp.host"] = "smtpgw.cdc.gov"
        props["mail.smtp.port"] = 25
        val session = Session.getInstance(props, null)

        EmailUtil.sendEmail(
            session,
            toEmail,
            payload.emailSubject,
            payload.htmlBody
        )
    }
}