package gov.cdc.ocio.processingstatusapi.model.reports

import com.google.gson.annotations.SerializedName

/**
 * Report data access object.  A report is an aggregate of smaller reports from each of the stages that make up a service
 * line or processing pipeline.
 *
 * @property uploadId String?
 * @property destinationId String?
 * @property eventType String?
 * @property reports List<StageReport>?
 */
class HL7v2Counts {

    @SerializedName("upload_id")
    var uploadId: String? = null

    @SerializedName("destination_id")
    var destinationId: String? = null

    @SerializedName("event_type")
    var eventType: String? = null

    @SerializedName("report_count")
    var reportCount: Int? = null
}
