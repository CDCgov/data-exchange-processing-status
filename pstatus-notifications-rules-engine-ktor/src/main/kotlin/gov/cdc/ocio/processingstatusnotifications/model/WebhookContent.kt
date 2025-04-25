package gov.cdc.ocio.processingstatusnotifications.model

import com.google.gson.GsonBuilder
import com.google.gson.ToNumberPolicy
import gov.cdc.ocio.messagesystem.models.ReportMessage
import gov.cdc.ocio.types.adapters.DateLongFormatTypeAdapter
import gov.cdc.ocio.types.adapters.InstantTypeAdapter
import gov.cdc.ocio.types.model.SubscriptionRule
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*


/**
 * Defines the content of webhook calls.
 *
 * @property subscriptionId String
 * @property subscriptionRule SubscriptionRule
 * @property report ReportMessage
 * @constructor
 */
data class WebhookContent(
    val subscriptionId: String,
    val subscriptionRule: SubscriptionRule,
    val report: ReportMessage
) {
    /**
     * Converts this object into a payload.
     *
     * @return String
     */
    fun toPayload(): Map<String, Any> {
        return mapOf(
            "subscriptionId" to subscriptionId,
            "subscriptionRule" to subscriptionRule,
            "triggered" to DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
            "report" to report,
        )
    }
}