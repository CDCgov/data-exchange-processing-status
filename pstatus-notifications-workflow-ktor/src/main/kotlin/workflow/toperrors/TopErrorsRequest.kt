package gov.cdc.ocio.processingnotifications.workflow.toperrors

import gov.cdc.ocio.processingnotifications.activity.DataRequest


/**
 * Represents a request to retrieve details about the most significant errors
 * within a specified time interval for a given data stream.
 *
 * This request is used in workflows to analyze and process errors related to data streams
 * identified by their unique ID and routing information. It helps in generating insights
 * like failed deliveries, delayed uploads, and other metrics for monitoring purposes.
 *
 * @property dataStreamId Unique identifier of the data stream for which errors need to be analyzed.
 * @property dataStreamRoute Route or path of the data stream, used for categorization or routing purposes.
 * @property dayInterval Number of days to analyze for errors, used as the time window for the operation.
 */
data class TopErrorsRequest(
    val dataStreamId: String,
    val dataStreamRoute: String,
    val dayInterval : Int
) : DataRequest