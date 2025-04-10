package gov.cdc.ocio.processingnotifications.workflow.digestcounts

import gov.cdc.ocio.processingnotifications.utils.CronUtils
import gov.cdc.ocio.types.email.EmailBuilder
import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import org.apache.commons.io.FileUtils
import gov.cdc.ocio.types.extensions.toHumanReadable
import java.time.Duration
import java.util.*
import kotlin.math.roundToLong


/**
 * Convenience class for building an upload digest of counts email.
 *
 * @property workflowId String
 * @property cronSchedule String
 * @property dataStreamIds List<String>
 * @property dataStreamRoutes List<String>
 * @property jurisdictions List<String>
 * @property runDateUtc String
 * @property digestCounts UploadDigestCounts
 * @property uploadMetrics UploadMetrics
 * @property deliveryLatenciesInSec List<Long>
 * @constructor
 */
class UploadDigestCountsEmailBuilder(
    private val workflowId: String,
    private val cronSchedule: String,
    private val dataStreamIds: List<String>,
    private val dataStreamRoutes: List<String>,
    private val jurisdictions: List<String>,
    private val runDateUtc: String,
    private val digestCounts: UploadDigestCounts,
    private val uploadMetrics: UploadMetrics,
    private val deliveryLatenciesInSec: List<Long>
) {

    fun build(): String {
        val uploadCounts = digestCounts.digest

        val cronScheduleDesc = CronUtils.description(cronSchedule)?.replaceFirstChar { it.uppercaseChar() } ?: "Unknown"
        val dataStreamIdsDesc = dataStreamIds.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "All"
        val dataStreamRoutesDesc = dataStreamRoutes.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "All"
        val jurisdictionsDesc = jurisdictions.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "All"

        val minUploadTimeDurationDesc = Duration.ofSeconds(
            uploadMetrics.minDelta).toHumanReadable()
        val maxUploadTimeDurationDesc = Duration.ofSeconds(
            uploadMetrics.maxDelta).toHumanReadable()
        val meanUploadTimeDurationDesc = Duration.ofMillis(
            (uploadMetrics.meanDelta * 1000).roundToLong()).toHumanReadable()
        val medianUploadTimeDurationDesc = Duration.ofMillis(
            (uploadMetrics.medianDelta * 1000).roundToLong()).toHumanReadable()

        // Generate the delivery latency chart and convert it to a base64 encoded PNG.
        val imageBase64String = runCatching {
            val chartInBytes = DeliveryLatencyChart(deliveryLatenciesInSec, 800, 400)
                .toPngAsByteArray()
            // Convert the byte array to a Base64 string
            return@runCatching Base64.getEncoder().encodeToString(chartInBytes)
        }.getOrNull()

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
                    tr {
                        td { +"Run Schedule" }
                        td { strong { +cronScheduleDesc } }
                    }
                    tr {
                        td { +"Data Stream ID(s)" }
                        td { strong { +dataStreamIdsDesc } }
                    }
                    tr {
                        td { +"Data Stream Route(s)" }
                        td { strong { +dataStreamRoutesDesc } }
                    }
                    tr {
                        td { +"Jurisdiction(s)" }
                        td { strong { +jurisdictionsDesc } }
                    }
                }
                h3 { +"Upload Metrics" }
                table(classes = "stylish-table") {
                    thead {
                        tr {
                            th { +"Category" }
                            th { +"Min" }
                            th { +"Max" }
                            th { +"Average (Mean)" }
                            th { +"Median" }
                        }
                    }
                    tr {
                        td { +"Upload Duration" }
                        td { +minUploadTimeDurationDesc }
                        td { +maxUploadTimeDurationDesc }
                        td { +meanUploadTimeDurationDesc }
                        td { +medianUploadTimeDurationDesc }
                    }
                    tr {
                        td { +"Upload File Size" }
                        td { +FileUtils.byteCountToDisplaySize(uploadMetrics.minFileSize) }
                        td { +FileUtils.byteCountToDisplaySize(uploadMetrics.maxFileSize) }
                        td { +FileUtils.byteCountToDisplaySize(uploadMetrics.meanFileSize) }
                        td { +FileUtils.byteCountToDisplaySize(uploadMetrics.medianFileSize) }
                    }
                }
                h3 { +"Delivery Latencies"}
                p {
                    +"The delivery latency is the time from when the an upload starts to when uploaded file is finished being delivered. "
                    +"The chart below shows a histogram of the delivery times, categorized by latency buckets. The frequency is the count "
                    +"of delivery times that fall in that delivery time range."
                }
                if (imageBase64String != null) {
                    img(src = "data:image/png;base64,$imageBase64String", alt = "Latency Distribution Chart")
                } else {
                    p { +"No data." }
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
                                th { +"Uploads Completed" }
                            }
                        }
                        tbody {
                            uploadCounts.forEach { (dataStreamId, dataStreamRoutes) ->
                                dataStreamRoutes.forEach { (dataStreamRoute, jurisdictions) ->
                                    val uploadsCompleted = jurisdictions.values.sumOf { it.uploadCompleted }
                                    tr {
                                        td { +dataStreamId }
                                        td { +dataStreamRoute }
                                        td { +jurisdictions.size.toString() }
                                        td { +uploadsCompleted.toString() }
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
                                th { +"Completed" }
                                th { +"In-Progress"}
                                th { +"Delivered" }
                            }
                        }
                        tbody {
                            uploadCounts.forEach { (dataStreamId, dataStreamRoutes) ->
                                dataStreamRoutes.forEach { (dataStreamRoute, jurisdictions) ->
                                    jurisdictions.forEach { (jurisdiction, count) ->
                                        val inProgress = count.uploadCompleted - count.uploadStarted
                                        val delivered = count.deliverySucceeded - count.deliveryFailed
                                        tr {
                                            td { +dataStreamId }
                                            td { +dataStreamRoute }
                                            td { +jurisdiction }
                                            td { +count.uploadCompleted.toString() }
                                            td { +inProgress.toString()}
                                            td { +delivered.toString() }
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