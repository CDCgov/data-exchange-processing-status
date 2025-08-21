package gov.cdc.ocio.processingnotifications.workflow.deadlinecheck

import gov.cdc.ocio.processingnotifications.activity.DataResponse
import gov.cdc.ocio.processingnotifications.query.DeadlineCompliance


/**
 * Represents the response of a deadline compliance check.
 *
 * This data class contains the results of checking compliance with a specified upload deadline
 * for a data stream or workflow. It provides detailed information about jurisdictions that either
 * did not comply with the deadline or are summarized using metadata.
 *
 * @property deadlineCompliance The results of the compliance check, including details on missing and late jurisdictions.
 * @property jurisdictionCounts Metadata about uploads for each jurisdiction, such as the count of uploads and the last upload timestamp.
 */
data class DeadlineCheckResponse(
    val deadlineCompliance: DeadlineCompliance,
    val jurisdictionCounts: Map<String, JurisdictionFacts>
) : DataResponse