package gov.cdc.ocio.processingnotifications.workflow.toperrors

import gov.cdc.ocio.processingnotifications.model.workflowHeader
import gov.cdc.ocio.processingnotifications.model.workflowFooter
import gov.cdc.ocio.notificationdispatchers.email.EmailBuilder
import gov.cdc.ocio.processingnotifications.model.UploadInfo
import gov.cdc.ocio.processingnotifications.utils.CronUtils
import gov.cdc.ocio.types.utils.TimeUtils
import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter


class TopErrorsEmailBuilder(
    private val workflowId: String,
    private val cronSchedule: String,
    private val dataStreamId: String,
    private val dataStreamRoute: String,
    private val failedMetadataValidationCount: Int,
    private val failedDeliveryCount: Int,
    private val delayedUploads: List<UploadInfo>,
    private val delayedDeliveries: List<UploadInfo>,
    private val abandonedUploads: List<UploadInfo>,
    private val daysInterval: Int
) {
    private val timestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z").withZone(ZoneId.of("UTC"))

    private fun getTimestampDesc(timestamp: Instant?): String {
        return runCatching {
            timestampFormatter.format(timestamp)
        }.getOrDefault("Unknown")
    }

    private fun getElapsedTime(timestamp: Instant?): String {
        if (timestamp == null) return "Unknown"
        val secondsAgo = (System.currentTimeMillis() - timestamp.toEpochMilli()) / 1000
        return TimeUtils.formatRelativeTime(secondsAgo)
    }

    /**
     * Builds the HTML string of the email body
     */
    fun build(): String {
        val cronScheduleDesc = CronUtils.description(cronSchedule)?.replaceFirstChar { it.uppercaseChar() } ?: "Unknown"
        val jurisdictionsDesc = "All"

        val content = buildString {
            appendHTML().body {
                workflowHeader()
                h2 { +"Upload Issues in the last $daysInterval days" }
                h3 { +"Overview" }
                table {
                    tr {
                        td { +"Subscription ID" }
                        td { strong { +workflowId } }
                    }
                    tr {
                        td { +"Run Schedule" }
                        td { strong { +cronScheduleDesc } }
                    }
                    tr {
                        td { +"Data Stream ID" }
                        td { strong { +dataStreamId } }
                    }
                    tr {
                        td { +"Data Stream Route" }
                        td { strong { +dataStreamRoute } }
                    }
                    tr {
                        td { +"Jurisdiction(s)" }
                        td { strong { +jurisdictionsDesc } }
                    }
                }
                br { }
                table(classes = "stylish-table") {
                    thead {
                        tr {
                            th { +"Category" }
                            th { +"Count" }
                        }
                    }
                    tr {
                        td { +"Failed Metadata Validation" }
                        td { +"$failedMetadataValidationCount" }
                    }
                    tr {
                        td { +"Failed Deliveries" }
                        td { +"$failedDeliveryCount" }
                    }
                    tr {
                        td { +"Delayed Uploads [1]" }
                        td { +"${delayedUploads.size}" }
                    }
                    tr {
                        td { +"Delayed Deliveries [2]" }
                        td { +"${delayedDeliveries.size}" }
                    }
                    tr {
                        td { +"Abandoned Uploads [3]" }
                        td { +"${abandonedUploads.size}" }
                    }
                    tr {
                        td { strong { +"Total" } }
                        td { strong { +"${failedMetadataValidationCount + failedDeliveryCount + delayedUploads.size + delayedDeliveries.size + abandonedUploads.size}" } }
                    }
                }
                div {
                    p {
                        +"[1] A "
                        b { +"delayed upload" }
                        +" is an upload that was initiated but has not completed in more than one hour. Delayed uploads that are more than a week old will not be shown."
                    }
                    p {
                        +"[2] A "
                        b { +"delayed delivery" }
                        +" is an upload that was completed but has not completed delivery in more than one hour since the upload started."
                    }
                    p {
                        +"[3] An "
                        b { +"abandoned upload" }
                        +" is an upload that was initiated more than a week ago and has still not completed. Abandoned uploads that are more than a month old will not be shown."
                    }
                }
                br { }
                h3 { +"Delayed Uploads" }
                if (delayedUploads.isEmpty()) {
                    p { +"No delayed uploads found." }
                } else {
                    table(classes = "stylish-table") {
                        thead {
                            tr {
                                th { +"Filename" }
                                th { +"Upload ID" }
                                th { +"Upload Start Time" }
                                th { +"Elapsed Time" }
                            }
                        }
                        delayedUploads.forEach {
                            tr {
                                td { +(it.filename ?: "Unknown") }
                                td { +it.uploadId }
                                td { +getTimestampDesc(it.uploadStartTime) }
                                td { +getElapsedTime(it.uploadStartTime) }
                            }
                        }
                    }
                }
                br { }
                h3 { +"Delayed Deliveries" }
                if (delayedDeliveries.isEmpty()) {
                    p { +"No delayed deliveries found." }
                } else {
                    table(classes = "stylish-table") {
                        thead {
                            tr {
                                th { +"Upload ID" }
                                th { +"Filename" }
                                th { +"Upload Start Time" }
                                th { +"Elapsed Time" }
                            }
                        }
                        delayedDeliveries.forEach {
                            tr {
                                td { +it.uploadId }
                                td { +(it.filename ?: "Unknown") }
                                td { +getTimestampDesc(it.uploadStartTime) }
                                td { +getElapsedTime(it.uploadStartTime) }
                            }
                        }
                    }
                }
                br { }
                h3 { +"Abandoned Uploads" }
                if (abandonedUploads.isEmpty()) {
                    p { +"No abandoned uploads found." }
                } else {
                    table(classes = "stylish-table") {
                        thead {
                            tr {
                                th { +"Filename" }
                                th { +"Upload ID" }
                                th { +"Upload Start Time" }
                                th { +"Elapsed Time" }
                            }
                        }
                        abandonedUploads.forEach {
                            tr {
                                td { +(it.filename ?: "Unknown") }
                                td { +it.uploadId }
                                td { +getTimestampDesc(it.uploadStartTime) }
                                td { +getElapsedTime(it.uploadStartTime) }
                            }
                        }
                    }
                }
                workflowFooter()
            }
        }

        return EmailBuilder()
            .commonHeader(true)
            .htmlBody(content)
            .build()
    }
}