package gov.cdc.ocio.processingstatusapi.model

import com.google.gson.annotations.SerializedName

/**
 * Contains all the properties for the return response when a report has been successfully amended.
 *
 * @property reportId String
 * @property stageName String
 * @constructor
 */
data class AmendReportResult(@SerializedName("report_id") val reportId: String,
                             @SerializedName("stage_name") val stageName: String)