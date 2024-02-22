package gov.cdc.ocio.processingstatusapi.model.reports

import com.google.gson.annotations.SerializedName

/**
 * Report data access object.  A report is an aggregate of smaller reports from each of the stages that make up a service
 * line or processing pipeline.
 *
 * @property uploadId String?
 * @property dataStreamId String?
 * @property dataStreamRoute String?
 * @property reports List<StageReport>?
 */
class ReportDaoV2 {

    @SerializedName("upload_id")
    var uploadId: String? = null

    @SerializedName("data_stream_id")
    var dataStreamId: String? = null

    @SerializedName("data_stream_route")
    var dataStreamRoute: String? = null

    var reports: List<ReportV2>? = null
}
