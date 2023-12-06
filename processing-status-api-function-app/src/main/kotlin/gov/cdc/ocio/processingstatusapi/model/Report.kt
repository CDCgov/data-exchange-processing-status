package gov.cdc.ocio.processingstatusapi.model

import com.google.gson.annotations.SerializedName

/**
 * Report definition.
 *
 * @property id String?
 * @property reportId String?
 * @property uploadId String?
 * @property destinationId String?
 * @property eventType String?
 */
class Report {

    var id : String? = null

    @SerializedName("report_id")
    var reportId: String? = null

    @SerializedName("upload_id")
    var uploadId: String? = null

    @SerializedName("destination_id")
    var destinationId: String? = null

    @SerializedName("event_type")
    var eventType: String? = null

}
