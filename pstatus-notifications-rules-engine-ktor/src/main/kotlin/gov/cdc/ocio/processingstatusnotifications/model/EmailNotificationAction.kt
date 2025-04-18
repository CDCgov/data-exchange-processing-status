package gov.cdc.ocio.processingstatusnotifications.model

import gov.cdc.ocio.notificationdispatchers.email.EmailDispatcher
import gov.cdc.ocio.processingstatusnotifications.exception.BadRequestException
import gov.cdc.ocio.types.model.EmailNotification
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


class EmailNotificationAction(
    private val emailNotification: EmailNotification
) : NotificationAction, KoinComponent {

    private val emailService by inject<EmailDispatcher>()

    private val fromEmail = "donotreply@cdc.gov"
    private val fromName = "Do not reply (PHDO team)"

    /**
     * For emails, the payload should be [EmailContent].
     *
     * @param payload Any
     */
    override fun doNotify(payload: Any) {
        if (payload !is EmailContent) throw BadRequestException("Email payload is not in the expected format")

        emailService.send(
            emailNotification.emailAddresses,
            fromEmail,
            fromName,
            payload.emailSubject,
            payload.toHtml()
        )
    }
}
