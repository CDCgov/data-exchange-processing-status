package gov.cdc.ocio.processingnotifications.workflow.deadlinecheck

import gov.cdc.ocio.notificationdispatchers.email.EmailBuilder
import gov.cdc.ocio.processingnotifications.model.workflowHeader
import gov.cdc.ocio.processingnotifications.model.workflowFooter
import gov.cdc.ocio.processingnotifications.query.DeadlineCheckResults
import gov.cdc.ocio.processingnotifications.utils.CronUtils
import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter


data class JurisdictionFacts(val count: Int, val lastUpload: Instant?)

/**
 * Responsible for constructing email notifications regarding upload deadline checks.
 *
 * This class generates an HTML email report for a specific workflow, summarizing the results of an upload
 * deadline check for jurisdictions within a data stream. It provides details about missing and late
 * jurisdictions, along with relevant upload statistics and deadline information.
 *
 * @property workflowId The unique identifier for the workflow associated with this email.
 * @property cronSchedule The cron expression representing the schedule for the workflow run.
 * @property triggered The timestamp indicating when the deadline check was triggered.
 * @property dataStreamId The identifier of the data stream associated with the deadline check.
 * @property dataStreamRoute The route related to the data stream.
 * @property expectedJurisdictions List of jurisdictions expected to participate in the upload.
 * @property deadlineCheckResults Results of the deadline check, containing information about missing and late jurisdictions.
 * @property deadlineTime The specific deadline time (HH:mm:ss) in UTC for uploads to be completed.
 * @property jurisdictionCounts Map of jurisdictions to their respective upload counts and last upload timestamps.
 */
class DeadlineCheckEmailBuilder(
    private val workflowId: String,
    private val cronSchedule: String,
    private val triggered: Long,
    private val dataStreamId: String,
    private val dataStreamRoute: String,
    private val expectedJurisdictions: List<String>,
    private val deadlineCheckResults: DeadlineCheckResults,
    private val deadlineTime: LocalTime,
    private val jurisdictionCounts: Map<String, JurisdictionFacts>
) {

    private val standardFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z").withZone(ZoneId.of("UTC"))
    private val deadlineFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    fun build(): String {
        val cronScheduleDesc = CronUtils.description(cronSchedule)?.replaceFirstChar { it.uppercaseChar() } ?: "Unknown"
        val triggeredDesc = standardFormatter.format(Instant.ofEpochMilli(triggered))
        val expectedJurisdictionsDesc = expectedJurisdictions.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "None"
        val missingJurisdictionsDesc = deadlineCheckResults.missingJurisdictions.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "None"
        val lateJurisdictionsDesc = deadlineCheckResults.lateJurisdictions.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "None"
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
                h3 { +"Missing or Late Jurisdictions" }
                p {
                    +"The following jurisdictions did not provide any uploads by the expected deadline of "
                    +"$deadlineTimeDesc starting at midnight." }
                table {
                    tr {
                        td { +"Expected Jurisdiction(s)" }
                        td { strong { +expectedJurisdictionsDesc } }
                    }
                    tr {
                        td { +"Missing Jurisdiction(s) [1]" }
                        td { strong { +missingJurisdictionsDesc } }
                    }
                    tr {
                        td { +"Late Jurisdiction(s) [2]" }
                        td { strong { +lateJurisdictionsDesc } }
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
                        +"[1] The "
                        b { +"missing jurisdiction(s)" }
                        +" are those that did not provide any data uploads by the expected deadline of $deadlineTimeDesc starting at midnight."
                    }
                    p {
                        +"[2] The "
                        b { +"late jurisdiction(s)" }
                        +" are those that provided an upload, but after the deadline."
                    }
                    p {
                        +"If you believe there is an error, please contact the PHDO Processing Status (PS) API "
                        +"support team at "
                        a(href = "mailto:DEXUploadAPI@cdc.gov") {
                            +"PHDO Upload API Support"
                        }
                        +"."
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