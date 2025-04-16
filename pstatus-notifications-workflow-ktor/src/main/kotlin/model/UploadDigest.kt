package gov.cdc.ocio.processingnotifications.model

import gov.cdc.ocio.types.notification.Notifiable
import kotlinx.html.*
import kotlinx.html.stream.appendHTML

data class UploadDigest(val counts: Map<String, Map<String, Map<String, Int>>>, val timestamp: String) : Notifiable {
    override fun subject(): String {
        return "PHDO DAILY UPLOAD DIGEST COUNTS NOTIFICATION"
    }

    override fun buildEmailBody(): String {
        return buildString {
            appendHTML().html {
                body {
                    h2 { +"Daily Upload Digest for Data Streams" }
                    div { +"Date: ${timestamp} (12:00:00am through 12:59:59pm UTC)" }
                    br
                    h3 { +"Summary" }
                    table {
                        if (counts.isEmpty()) {
                            +"No uploads found."
                        } else {
                            thead {
                                tr {
                                    th { +"Data Stream ID" }
                                    th { +"Data Stream Route" }
                                    th { +"Jurisdictions" }
                                    th { +"Upload Counts" }
                                }
                            }
                            tbody {
                                counts.forEach { (dataStreamId, dataStreamRoutes) ->
                                    dataStreamRoutes.forEach { (dataStreamRoute, jurisdictions) ->
                                        tr {
                                            td { +dataStreamId }
                                            td { +dataStreamRoute }
                                            td { +jurisdictions.size.toString() }
                                            td { +jurisdictions.values.sum().toString() }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (counts.isNotEmpty()) {
                        br
                        h3 { +"Details" }
                        table {
                            thead {
                                tr {
                                    th { +"Data Stream ID" }
                                    th { +"Data Stream Route" }
                                    th { +"Jurisdiction" }
                                    th { +"Upload Counts" }
                                }
                            }
                            tbody {
                                counts.forEach { (dataStreamId, dataStreamRoutes) ->
                                    dataStreamRoutes.forEach { (dataStreamRoute, jurisdictions) ->
                                        jurisdictions.forEach { (jurisdiction, count) ->
                                            tr {
                                                td { +dataStreamId }
                                                td { +dataStreamRoute }
                                                td { +jurisdiction }
                                                td { +count.toString() }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    p {
                        +("Subscriptions to this email are managed by the Public Health Data Observability (PHDO) "
                                + "Processing Status (PS) API.  Use the PS API GraphQL interface to unsubscribe."
                                )
                    }
                    p {
                        a(href = "https://cdcgov.github.io/data-exchange/") { +"Click here" }
                        +" for more information."
                    }
                }
            }
        }
    }

    override fun buildWebhookBody(): Any {
        TODO("Not yet implemented")
    }

}