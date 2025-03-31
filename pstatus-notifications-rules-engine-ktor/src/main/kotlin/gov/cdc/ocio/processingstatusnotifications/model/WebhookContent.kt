package gov.cdc.ocio.processingstatusnotifications.model

import com.google.gson.GsonBuilder
import com.google.gson.ToNumberPolicy
import gov.cdc.ocio.messagesystem.models.CreateReportMessage
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
 * @property report CreateReportMessage
 * @constructor
 */
data class WebhookContent(
    val subscriptionId: String,
    val subscriptionRule: SubscriptionRule,
    val report: CreateReportMessage
) {
    /**
     * Converts this object into a JSON payload.
     *
     * @return String
     */
    fun toJson(): String {
        val gson =
            GsonBuilder()
                .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
                .registerTypeAdapter(Date::class.java, DateLongFormatTypeAdapter())
                .registerTypeAdapter(Instant::class.java, InstantTypeAdapter())
                .create()

        val webhookContent = mapOf(
            "subscriptionId" to subscriptionId,
            "subscriptionRule" to subscriptionRule,
            "triggered" to DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
            "report" to report,
        )
        return gson.toJson(webhookContent)
    }
}