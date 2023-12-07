package gov.cdc.ocio.processingstatusapi.model

import com.google.gson.annotations.SerializedName

/**
 * Contains all the properties for the return response when a report has been successfully amended.
 *
 * @property stageReportId String
 * @property stageName String
 * @constructor
 */
data class CreateReportResult(@SerializedName("stage_report_id") val stageReportId: String,
                              @SerializedName("stage_name") val stageName: String)