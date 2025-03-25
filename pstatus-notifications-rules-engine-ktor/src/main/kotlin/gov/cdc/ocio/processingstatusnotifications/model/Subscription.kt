package gov.cdc.ocio.processingstatusnotifications.model

import gov.cdc.ocio.processingstatusnotifications.model.report.ReportMessage
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
    val subscriptionRule: SubscriptionRule,
    val notification: Notification
) {
    fun doNotify(report: ReportMessage) {
        when (notification.notificationType) {
            SubscriptionType.EMAIL -> {
                CoroutineScope(Dispatchers.Default).launch {
                    // Fire and forget and don't wait for the email to be sent since the SMTP server may be down and if
                    // it is, this will wait until that times out and that can be 10s of seconds.
                    notification.doNotify(EmailPayload("MBK was here", "upload id = ${report.uploadId}"))
                }
            }

            SubscriptionType.WEBHOOK -> {
                CoroutineScope(Dispatchers.Default).launch {
                    // Don't wait for a response to the call to the webhook.
                    notification.doNotify("todo")
                }
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Subscription

        if (subscriptionRule != other.subscriptionRule) return false
        if (notification != other.notification) return false

        return true
    }

    override fun hashCode(): Int {
        var result = subscriptionRule.hashCode()
        result = 31 * result + notification.hashCode()
        return result
    }

}