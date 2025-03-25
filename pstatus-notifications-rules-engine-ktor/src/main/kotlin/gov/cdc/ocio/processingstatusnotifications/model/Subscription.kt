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
}