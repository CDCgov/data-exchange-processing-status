package gov.cdc.ocio.processingnotifications.workflow.toperrors

import gov.cdc.ocio.processingnotifications.activity.DataResponse
import gov.cdc.ocio.processingnotifications.model.UploadInfo


/**
 * Represents the aggregated data and statistics of the most significant issues encountered
 * in a data processing workflow over a specific time interval.
 *
 * This response provides details about counts and lists of various types of issues,
 * such as metadata validation failures, failed deliveries, delayed uploads, delayed deliveries,
 * and abandoned uploads. It is used to facilitate error reporting, monitoring, and troubleshooting
 * workflows by providing actionable insights.
 *
 * @property failedMetadataValidationCount The total count of uploads that failed metadata validation.
 * @property failedDeliveryCount The total count of uploads that failed during the delivery process.
 * @property delayedUploads A list of uploads that were initiated but have been delayed for more than a specific duration.
 * @property delayedDeliveries A list of uploads that were successfully completed but delayed in the delivery phase.
 * @property abandonedUploads A list of uploads that were initiated but abandoned without completion for an extended period.
 */
data class TopErrorsResponse(
    val failedMetadataValidationCount: Int,
    val failedDeliveryCount: Int,
    val delayedUploads: List<UploadInfo>,
    val delayedDeliveries: List<UploadInfo>,
    val abandonedUploads: List<UploadInfo>,
) : DataResponse