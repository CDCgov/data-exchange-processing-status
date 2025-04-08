package gov.cdc.ocio.processingnotifications.workflow.digestcounts

import kotlinx.html.*
import kotlinx.html.stream.appendHTML


/**
 * Convenience class for building an upload digest of counts email.
 *
 * @property runDateUtc String
 * @property digestCounts UploadDigestCounts
 * @constructor
 */
class UploadDigestCountsEmailBuilder(
    private val runDateUtc: String,
    private val digestCounts: UploadDigestCounts
) {

    fun build(): String {
        val uploadCounts = digestCounts.digest

        return buildString {
            appendHTML().html {
                body {
                    h2 { +"Daily Upload Digest for Data Streams" }
                    div { +"Date: $runDateUtc (12:00:00am through 12:59:59pm UTC)" }
                    br
                    h3 { +"Summary" }
                    table {
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
}