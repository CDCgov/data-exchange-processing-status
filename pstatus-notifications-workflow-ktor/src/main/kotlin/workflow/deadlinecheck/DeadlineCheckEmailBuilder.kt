package gov.cdc.ocio.processingnotifications.workflow.deadlinecheck

import gov.cdc.ocio.processingnotifications.model.workflowFooter
import gov.cdc.ocio.notificationdispatchers.email.EmailBuilder
import kotlinx.html.*
import kotlinx.html.stream.appendHTML


/**
 * This class is responsible for building HTML email content related to deadline checks for data stream uploads.
 * It generates a structured email containing various sections like upload metrics, durations, and workflow details.
 *
 * @property workflowId Unique identifier of the workflow associated with the email.
 * @property cronSchedule Cron schedule string representing the periodic job triggering timings.
 * @property triggered Timestamp marking when the job was triggered.
 * @property dataStreamId Identifier for the related data stream.
 * @property dataStreamRoute The route or path associated with the data stream.
 * @property expectedJurisdictions List of jurisdictions expected for data processing.
 */
class DeadlineCheckEmailBuilder(
    private val workflowId: String,
    private val cronSchedule: String,
    private val triggered: Long,
    private val dataStreamId: String,
    private val dataStreamRoute: String,
    private val expectedJurisdictions: List<String>
) {

    fun build(): String {


        val content = buildString {
            appendHTML().body {
                div {
                    span(classes = "bold-uppercase") { +"\u271A Public Health" }
                    span(classes = "uppercase") { +" Data Operations" }
                }
                hr {  }
                h2 { +"Upload Digest for Data Streams" }
                div { +"Run: $triggered" }
                h3 { +"Overview" }
                table {
                    tr {
                        td { +"Workflow ID" }
                        td { strong { +workflowId } }
                    }
                }
                h3 { +"Upload Metrics" }
                p {
                    +"The "
                    b { +"Upload Duration"}
                    +" is the time from when the upload is initiated, including the metadata verification step to when"
                    +" the upload completes."
                    +" The "
                    b { +"Delivery Duration" }
                    +" includes the latency that may occur between the completion of the upload and the completion of"
                    +" the delivery."
                }
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
                        td { strong { +"Upload Duration" } }

                    }
                    tr {
                        td { strong { +"Delivery Duration" } }

                    }
                    tr {
                        td { strong { +"Total Duration" } }

                    }
                    tr {
                        td { strong { +"Upload File Size" } }
                    }
                }
                h3 { +"Total Duration"}
                p {
                    +"The "
                    b { +"Total Duration" }
                    +" is the time from when an upload starts to when the uploaded file has been delivered. "
                    +"The chart below shows a histogram of the durations, categorized by time buckets. The "
                    b { +"Number of Uploads" }
                    +" is the count of durations that fall within the duration bucket time range (duration bins)."
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