package gov.cdc.ocio.processingnotifications.workflow.deadlinecheck

import gov.cdc.ocio.processingnotifications.activity.DataRequest
import java.time.LocalTime

/**
 * Represents a request to check upload deadlines for a specified data stream and its jurisdictions.
 *
 * This data class is used to initiate a deadline compliance check for a given data stream and its
 * associated jurisdictions, specifying the expected upload deadline time and the jurisdictions to monitor.
 *
 * @property dataStreamId The unique identifier for the data stream for which the deadline check is being requested.
 * @property dataStreamRoute The route associated with the data stream, providing additional context for the check.
 * @property expectedJurisdictions The list of jurisdictions expected to participate in the data uploads.
 * @property deadlineTime The specific time of the day (in HH:mm:ss format) by which the jurisdictions are expected to complete their uploads.
 */
data class DeadlineCheckRequest(
    val dataStreamId: String,
    val dataStreamRoute: String,
    val expectedJurisdictions: List<String>,
    val deadlineTime: LocalTime
) : DataRequest