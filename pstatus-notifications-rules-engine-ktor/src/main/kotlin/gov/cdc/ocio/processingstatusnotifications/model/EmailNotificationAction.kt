package gov.cdc.ocio.processingstatusnotifications.model

import gov.cdc.ocio.notificationdispatchers.model.EmailNotificationContent
import gov.cdc.ocio.notificationdispatchers.NotificationDispatcher
import gov.cdc.ocio.processingstatusnotifications.exception.BadRequestException
import gov.cdc.ocio.types.model.EmailNotification
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


class EmailNotificationAction(
    private val emailNotification: EmailNotification
) : NotificationAction, KoinComponent {

    private val dispatchWorker by inject<NotificationDispatcher>()

    private val fromEmail = "donotreply@cdc.gov"
    private val fromName = "Do not reply (PHDO team)"

    /**
     * For emails, the payload should be [EmailContent].
     *
     * @param payload Any
     */
    override fun doNotify(payload: Any) {
        if (payload !is EmailContent) throw BadRequestException("Email payload is not in the expected format")

        dispatchWorker.send(
            EmailNotificationContent(
                emailNotification.emailAddresses,
                fromEmail,
                fromName,
                payload.emailSubject,
                payload.toHtml()
            )
        )
    }
}
