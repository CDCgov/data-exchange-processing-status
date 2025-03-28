package gov.cdc.ocio.processingstatusnotifications.model

import com.google.gson.GsonBuilder
import com.google.gson.ToNumberPolicy
import gov.cdc.ocio.processingstatusnotifications.model.report.ReportMessage
import gov.cdc.ocio.types.adapters.DateLongFormatTypeAdapter
import gov.cdc.ocio.types.adapters.InstantTypeAdapter
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
                .registerTypeAdapter(Instant::class.java, InstantTypeAdapter())
                .create()
        val jurisdiction = subscriptionRule.jurisdiction ?: "All"
        val mvelCondition = subscriptionRule.mvelRuleCondition

        val reportJson = gson.toJson(report)

        return buildString {
            appendHTML().html {
                head {
                    style {
                        +"""
                            body {
                                font-family: Arial, sans-serif;
                                margin: 20px;
                            }
                            .json-container {
                                background: #ededed;
                                color: #000000;
                                padding: 15px;
                                border-radius: 8px;
                                overflow-x: auto;
                                font-family: 'Courier New', monospace;
                                white-space: pre-wrap;
                                word-wrap: break-word;
                            }
                        """.trimIndent()
                    }
                }
                body {
                    h2 { +"Rule: ${subscriptionRule.ruleDescription}" }
                    div {
                        +"Subscription ID: "
                        b { +subscriptionId }
                    }
                    div {
                        +"Data Stream ID: "
                        b { +subscriptionRule.dataStreamId }
                    }
                    div {
                        +"Data Stream Route: "
                        b { +subscriptionRule.dataStreamRoute }
                    }
                    div {
                        +"Jurisdiction: "
                        b { +jurisdiction }
                    }
                    div {
                        +"Triggered: "
                        b { +DateTimeFormatter.ISO_INSTANT.format(Instant.now()) }
                    }
                    div {
                        br { b { +"Trigger Condition (MVEL notation):" } }
                        br {
                            pre(classes = "json-container") {
                                code {
                                    +mvelCondition
                                }
                            }
                        }
                    }
                    div { h3 { +"Triggering Report:" } }
                    pre(classes = "json-container") { code { +reportJson } }
                }
            }
        }
    }
}