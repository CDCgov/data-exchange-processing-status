package gov.cdc.ocio.processingstatusnotifications.model

import com.google.gson.GsonBuilder
import com.google.gson.ToNumberPolicy
import gov.cdc.ocio.messagesystem.models.ReportMessage
import gov.cdc.ocio.types.adapters.DateLongFormatTypeAdapter
import gov.cdc.ocio.types.adapters.InstantTypeAdapter
import gov.cdc.ocio.types.email.EmailBuilder
import gov.cdc.ocio.types.model.SubscriptionRule
import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*


/**
 * Defines the content of email calls.
 *
 * @property subscriptionId String
 * @property subscriptionRule SubscriptionRule
 * @property report ReportMessage
 * @property emailSubject String
 * @constructor
 */
data class EmailContent(
    val subscriptionId: String,
    val subscriptionRule: SubscriptionRule,
    val report: ReportMessage,
    val emailSubject: String
) {
    fun toHtml(): String {
        val gson =
            GsonBuilder()
                .setPrettyPrinting()
                .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
                .registerTypeAdapter(Date::class.java, DateLongFormatTypeAdapter())
                .registerTypeAdapter(Instant::class.java, InstantTypeAdapter(asEpoch = false))
                .create()
        val jurisdiction = subscriptionRule.jurisdiction ?: "All"
        val mvelCondition = subscriptionRule.mvelRuleCondition

        val reportJson = gson.toJson(report)

        val content = buildString {
            appendHTML().html {
                body {
                    div {
                        span(classes = "bold-uppercase") { +"\u271A Public Health" }
                        span(classes = "uppercase") { +" Data Operations" }
                    }
                    hr {  }
                    h3 { +"Rule: ${subscriptionRule.ruleDescription}" }
                    table {
                        tr {
                            td { +"Subscription ID" }
                            td { strong { +subscriptionId } }
                        }
                        tr {
                            td { +"Data Stream ID" }
                            td { strong { +subscriptionRule.dataStreamId } }
                        }
                        tr {
                            td { +"Data Stream Route" }
                            td { strong { +subscriptionRule.dataStreamRoute } }
                        }
                        tr {
                            td { +"Jurisdiction" }
                            td { strong { +jurisdiction } }
                        }
                        tr {
                            td { +"Triggered" }
                            td { strong { +DateTimeFormatter.ISO_INSTANT.format(Instant.now()) } }
                        }
                    }
                    div {
                        br {
                            b {
                                +"Trigger Condition ("
                                a(href = "http://mvel.documentnode.com/#basic-syntax") { +"MVEL notation" }
                                +"):"
                            }
                        }
                        br {
                            pre(classes = "json-container") {
                                code {
                                    +mvelCondition
                                }
                            }
                        }
                    }
                    div { b { +"Triggering Report:" } }
                    pre(classes = "json-container") { code { +reportJson } }
                    br { }
                    hr {  }
                    div {
                        small {
                            +("Subscriptions to this email are managed by the Public Health Data Operations (PHDO) "
                                    + "Processing Status (PS) API.  Use the PS API GraphQL interface to unsubscribe "
                                    + "with the subscription ID provided above."
                                    )
                        }
                    }
                }
            }
        }

        return EmailBuilder()
            .commonHeader(true)
            .htmlBody(content)
            .build()
    }
}