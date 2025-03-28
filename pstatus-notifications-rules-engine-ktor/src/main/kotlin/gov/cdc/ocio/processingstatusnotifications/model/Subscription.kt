package gov.cdc.ocio.processingstatusnotifications.model

import gov.cdc.ocio.processingstatusnotifications.model.report.ReportMessage
import gov.cdc.ocio.types.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


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
                CoroutineScope(Dispatchers.Default).launch {
                    // Fire and forget and don't wait for the email to be sent since the SMTP server may be down and if
                    // it is, this will wait until that times out and that can be 10s of seconds.
                    val notificationAction = EmailNotificationAction(notification as EmailNotification)
                    val emailContent = EmailContent(
                        subscriptionId,
                        subscriptionRule,
                        report,
                        "Triggered: ${subscriptionRule.ruleDescription}"
                    )
                    notificationAction.doNotify(emailContent)
                }
            }

            NotificationType.WEBHOOK -> {
                CoroutineScope(Dispatchers.Default).launch {
                    val notificationAction = WebhookNotificationAction(notification as WebhookNotification)
                    val webhookContent = WebhookContent(subscriptionId, subscriptionRule, report)

                    // Don't wait for a response to the call to the webhook.
                    notificationAction.doNotify(webhookContent)
                }
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