package gov.cdc.ocio.processingnotifications.workflow.deadlinecheck

import gov.cdc.ocio.notificationdispatchers.email.EmailBuilder
import gov.cdc.ocio.processingnotifications.model.workflowHeader
import gov.cdc.ocio.processingnotifications.model.workflowFooter
import gov.cdc.ocio.processingnotifications.utils.CronUtils
import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter


data class JurisdictionFacts(val count: Int, val lastUpload: Instant?)

/**
 * Class responsible for building HTML email content for deadline check notifications.
 * This includes an overview table summarizing details of the workflow run and
 * jurisdiction data, alongside custom styling for public health data notifications.
 *
 * @property workflowId The unique identifier for the workflow.
 * @property cronSchedule The cron expression defining the schedule of the workflow.
 * @property triggered The timestamp indicating when the workflow was triggered.
 * @property dataStreamId The unique identifier for the data stream.
 * @property dataStreamRoute The route associated with the data stream.
 * @property expectedJurisdictions The list of jurisdictions expected to have submitted data.
 * @property missingJurisdictions The list of jurisdictions identified as missing in the submission.
 * @property deadlineTime The time jurisdictions should have provided at least one upload by.
 */
class DeadlineCheckEmailBuilder(
    private val workflowId: String,
    private val cronSchedule: String,
    private val triggered: Long,
    private val dataStreamId: String,
    private val dataStreamRoute: String,
    private val expectedJurisdictions: List<String>,
    private val missingJurisdictions: List<String>,
    private val deadlineTime: LocalTime,
    private val jurisdictionCounts: Map<String, JurisdictionFacts>
) {

    private val standardFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z").withZone(ZoneId.of("UTC"))
    private val deadlineFormatter = DateTimeFormatter.ofPattern("hh:mm:ss")

    fun build(): String {
        val cronScheduleDesc = CronUtils.description(cronSchedule)?.replaceFirstChar { it.uppercaseChar() } ?: "Unknown"
        val triggeredDesc = standardFormatter.format(Instant.ofEpochMilli(triggered))
        val expectedJurisdictionsDesc = expectedJurisdictions.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "None"
        val missingJurisdictionsDesc = missingJurisdictions.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "None"
        val deadlineTimeDesc = deadlineFormatter.format(deadlineTime) + " UTC"

        val content = buildString {
            appendHTML().body {
                workflowHeader()
                h2 { +"Upload Deadline Check" }
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
                }
                h3 { +"Missing Jurisdictions" }
                p {
                    +"The following jurisdictions did not provide any uploads by the expected deadline of "
                    +"$deadlineTimeDesc starting at midnight." }
                p {
                    +"If you believe this is an error, please contact the PHDO Processing Status (PS) API "
                    +"support team at "
                    a(href = "mailto:DEXUploadAPI@cdc.gov") {
                        +"PHDO Upload API Support"
                    }
                    +"."
                }
                table {
                    tr {
                        td { +"Expected Jurisdiction(s)" }
                        td { strong { +expectedJurisdictionsDesc } }
                    }
                    tr {
                        td { +"Missing Jurisdiction(s)*" }
                        td { strong { +missingJurisdictionsDesc } }
                    }
                    tr {
                        td { +"Deadline" }
                        td { strong { +deadlineTimeDesc } }
                    }
                    tr {
                        td { +"Run" }
                        td { strong { +triggeredDesc } }
                    }
                }
                div {
                    p {
                        +"* The missing jurisdiction(s) are those that did not provide any data uploads by the expected deadline of $deadlineTimeDesc starting at midnight."
                    }
                }
                h3 { +"Reported Jurisdictions" }
                div {
                    if (jurisdictionCounts.isEmpty()) {
                        p { +"No jurisdictions reported." }
                    } else {
                        table(classes = "stylish-table") {
                            thead {
                                tr {
                                    th { +"Jurisdiction" }
                                    th { +"Count" }
                                    th { +"Last Upload" }
                                }
                            }
                            tr {
                                jurisdictionCounts.forEach { (jurisdiction, facts) ->
                                    val lastUpload = standardFormatter.format(facts.lastUpload) ?: "Unknown"
                                    td { +jurisdiction }
                                    td { +facts.count.toString() }
                                    td { +lastUpload }
                                }
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