package gov.cdc.ocio.processingnotifications.workflow.toperrors

import gov.cdc.ocio.processingnotifications.model.workflowHeader
import gov.cdc.ocio.processingnotifications.model.workflowFooter
import gov.cdc.ocio.notificationdispatchers.email.EmailBuilder
import gov.cdc.ocio.processingnotifications.model.UploadInfo
import gov.cdc.ocio.processingnotifications.utils.CronUtils
import kotlinx.html.*
import kotlinx.html.stream.appendHTML


class TopErrorsEmailBuilder(
    private val workflowId: String,
    private val cronSchedule: String,
    private val dataStreamId: String,
    private val dataStreamRoute: String,
    private val failedMetadataValidationCount: Int,
    private val failedDeliveryCount: Int,
    private val delayedUploads: List<UploadInfo>,
    private val delayedDeliveries: List<UploadInfo>,
    private val daysInterval: Int
) {

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
                        td { +"Delayed Uploads" }
                        td { +"${delayedUploads.size}" }
                    }
                    tr {
                        td { +"Delayed Deliveries" }
                        td { +"${delayedDeliveries.size}" }
                    }
                    tr {
                        td { strong { +"Total" } }
                        td { strong { +"${failedMetadataValidationCount + failedDeliveryCount + delayedUploads.size + delayedDeliveries.size}" } }
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
                                th { +"Upload ID" }
                                th { +"Filename" }
                            }
                        }
                        delayedUploads.forEach {
                            tr {
                                td { +it.uploadId }
                                td { +(it.filename ?: "Unknown") }
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
                            }
                        }
                        delayedDeliveries.forEach {
                            tr {
                                td { +it.uploadId }
                                td { +(it.filename ?: "Unknown") }
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