package gov.cdc.ocio.processingstatusnotifications.model

import gov.cdc.ocio.messagesystem.models.ReportMessage
import gov.cdc.ocio.types.model.*


/**
 * Subscription definition that is persisted.
 *
 * @property subscriptionRule SubscriptionRule1
 * @property notification Notification
 * @constructor
 */
data class Subscription(
    val subscriptionId: String,
    val subscriptionRule: SubscriptionRule,
    val notification: Notification
) {
    fun doNotify(report: ReportMessage) {
        when (notification.notificationType) {
            NotificationType.EMAIL -> {
                val notificationAction = EmailNotificationAction(notification as EmailNotification)
                val emailContent = EmailContent(
                    subscriptionId,
                    subscriptionRule,
                    report,
                    "Triggered: ${subscriptionRule.ruleDescription}"
                )
                notificationAction.doNotify(emailContent)
            }

            NotificationType.WEBHOOK -> {
                val notificationAction = WebhookNotificationAction(notification as WebhookNotification)
                val webhookContent = WebhookContent(subscriptionId, subscriptionRule, report)
                notificationAction.doNotify(webhookContent)
            }

            NotificationType.LOGGER -> {
                val notificationAction = LoggerNotificationAction()
                notificationAction.doNotify(report)
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Subscription) return false

        // IMPORTANT: do not include subscriptionId in the equals operator
        if (subscriptionRule != other.subscriptionRule) return false
        if (notification != other.notification) return false

        return true
    }

    override fun hashCode(): Int {
        // IMPORTANT: do not include subscriptionId in the hashCode
        var result = subscriptionRule.hashCode()
        result = 31 * result + notification.hashCode()
        return result
    }

}