package gov.cdc.ocio.processingstatusapi.model

import com.google.gson.annotations.SerializedName

/**
 * Contains all the properties for the return response when a report has been successfully created.
 *
 * @property reportId String?
 */
data class CreateReportResult(@SerializedName("report_id") val reportId: String)