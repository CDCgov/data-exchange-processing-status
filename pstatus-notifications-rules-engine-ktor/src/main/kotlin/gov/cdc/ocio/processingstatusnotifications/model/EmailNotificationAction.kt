package gov.cdc.ocio.processingstatusnotifications.model

import gov.cdc.ocio.processingstatusnotifications.dispatcher.EmailUtil
import gov.cdc.ocio.processingstatusnotifications.exception.BadRequestException
import gov.cdc.ocio.types.model.EmailNotification
import javax.mail.Session


class EmailNotificationAction(
    private val emailNotification: EmailNotification
) : NotificationAction {

    /**
     * For emails, the payload should be [EmailContent].
     *
     * @param payload Any
     */
    override fun doNotify(payload: Any) {
        if (payload !is EmailContent) throw BadRequestException("Email payload is not in the expected format")

        val toEmail = emailNotification.emailAddresses.joinToString(",")
        val props = System.getProperties()
        props["mail.smtp.host"] = "smtpgw.cdc.gov"
        props["mail.smtp.port"] = 25
        val session = Session.getInstance(props, null)

        EmailUtil.sendEmail(
            session,
            toEmail,
            payload.emailSubject,
            payload.toHtml()
        )
    }
}
