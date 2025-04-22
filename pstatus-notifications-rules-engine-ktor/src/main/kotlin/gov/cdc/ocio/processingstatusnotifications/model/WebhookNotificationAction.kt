package gov.cdc.ocio.processingstatusnotifications.model

import gov.cdc.ocio.notificationdispatchers.NotificationDispatcher
import gov.cdc.ocio.notificationdispatchers.model.WebhookNotificationContent
import gov.cdc.ocio.processingstatusnotifications.exception.BadRequestException
import gov.cdc.ocio.types.model.WebhookNotification
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


class WebhookNotificationAction(
    private val webhookNotification: WebhookNotification
) : NotificationAction, KoinComponent {

    private val notifications by inject<NotificationDispatcher>()

    /**
     * For webhooks, the content should be [WebhookContent].
     *
     * @param content Any
     */
    override fun doNotify(content: Any) {
        if (content !is WebhookContent)
            throw BadRequestException("Webhook content is not in the expected format")

        notifications.send(
            WebhookNotificationContent(
                webhookNotification.webhookUrl,
                content.toPayload()
            )
        )
    }
}
