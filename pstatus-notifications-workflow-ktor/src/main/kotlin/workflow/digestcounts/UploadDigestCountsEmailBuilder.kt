package gov.cdc.ocio.processingnotifications.workflow.digestcounts

import gov.cdc.ocio.types.email.EmailBuilder
import kotlinx.html.*
import kotlinx.html.stream.appendHTML


/**
 * Convenience class for building an upload digest of counts email.
 *
 * @property workflowId String
 * @property runDateUtc String
 * @property digestCounts UploadDigestCounts
 * @property timingMetrics TimingMetrics
 * @constructor
 */
class UploadDigestCountsEmailBuilder(
    private val workflowId: String,
    private val runDateUtc: String,
    private val digestCounts: UploadDigestCounts,
    private val timingMetrics: TimingMetrics
) {

    fun build(): String {
        val uploadCounts = digestCounts.digest

        val content = buildString {
            appendHTML().body {
                div {
                    span(classes = "bold-uppercase") { +"\u271A Public Health" }
                    span(classes = "uppercase") { +" Data Observability" }
                }
                hr {  }
                h2 { +"Daily Upload Digest for Data Streams" }
                div { +"Date: $runDateUtc (12:00:00am through 12:59:59pm UTC)" }
                h3 { +"Overview" }
                table {
                    tr {
                        td { +"Workflow ID" }
                        td { strong { +workflowId } }
                    }
                }
                h3 { +"Upload Metrics" }
                table {
                    tr {
                        td { +"Fastest" }
                        td { strong { +"${timingMetrics.minDelta} s" } }
                    }
                    tr {
                        td { +"Slowest" }
                        td { strong { +"${timingMetrics.maxDelta} s" } }
                    }
                    tr {
                        td { +"Mean (Average)" }
                        td { strong { +"${timingMetrics.meanDelta} s" } }
                    }
                    tr {
                        td { +"Median" }
                        td { strong { +"${timingMetrics.medianDelta} s" } }
                    }
                }
                h3 { +"Summary" }
                table(classes = "stylish-table") {
                    if (uploadCounts.isEmpty()) {
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
                            uploadCounts.forEach { (dataStreamId, dataStreamRoutes) ->
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
                if (uploadCounts.isNotEmpty()) {
                    h3 { +"Details" }
                    table(classes = "stylish-table") {
                        thead {
                            tr {
                                th { +"Data Stream ID" }
                                th { +"Data Stream Route" }
                                th { +"Jurisdiction" }
                                th { +"Upload Counts" }
                            }
                        }
                        tbody {
                            uploadCounts.forEach { (dataStreamId, dataStreamRoutes) ->
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
                br {  }
                br {  }
                div {
                    small {
                        +("Subscriptions to this email are managed by the Public Health Data Observability (PHDO) "
                                + "Processing Status (PS) API. Use the PS API GraphQL interface to unsubscribe "
                                + "with the workflow ID provided above. ")
                        a(href = "https://cdcgov.github.io/data-exchange/") { +"Click here" }
                        + " for more information."
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