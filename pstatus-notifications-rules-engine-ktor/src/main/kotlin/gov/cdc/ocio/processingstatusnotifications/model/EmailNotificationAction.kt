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

    private val notifications by inject<NotificationDispatcher>()

    private val fromEmail = "donotreply@cdc.gov"
    private val fromName = "Do not reply (PHDO team)"

    /**
     * For emails, the content should be [EmailContent].
     *
     * @param content Any
     */
    override fun doNotify(content: Any) {
        if (content !is EmailContent) throw BadRequestException("Email content is not in the expected format")

        notifications.send(
            EmailNotificationContent(
                emailNotification.emailAddresses,
                fromEmail,
                fromName,
                content.emailSubject,
                content.toHtml()
            )
        )
    }
}
