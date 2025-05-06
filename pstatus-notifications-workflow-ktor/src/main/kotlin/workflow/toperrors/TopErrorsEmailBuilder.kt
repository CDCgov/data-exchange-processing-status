package gov.cdc.ocio.processingnotifications.workflow.toperrors

import gov.cdc.ocio.processingnotifications.model.workflowHeader
import gov.cdc.ocio.processingnotifications.model.workflowFooter
import gov.cdc.ocio.notificationdispatchers.email.EmailBuilder
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
    private val delayedUploads: List<String>,
    private val delayedDeliveries: List<String>,
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
                        td { +"Workflow ID" }
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
                h3 { +"Total: ${failedMetadataValidationCount + failedDeliveryCount + delayedUploads.size + delayedDeliveries.size}" }
                ul {
                    li { +"Failed Metadata Validation: $failedMetadataValidationCount" }
                    li { +"Failed Deliveries: $failedDeliveryCount" }
                    li { +"Delayed Uploads: ${delayedUploads.size}" }
                    li { +"Delayed Deliveries: ${delayedDeliveries.size}" }
                }
                br { }
                h3 { +"Delayed Uploads" }
                ul {
                    delayedUploads.map { li { +it } }
                }
                br { }
                h3 { +"Delayed Deliveries" }
                ul {
                    delayedDeliveries.map { li { +it } }
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