package gov.cdc.ocio.processingstatusapi.model.reports

import com.google.gson.annotations.SerializedName
import gov.cdc.ocio.processingstatusapi.model.ServiceBusMessage

/**
 * Create a report service bus message.
 *
 * @property uploadId String?
 * @property destinationId String?
 * @property eventType String?
 * @property stageName String?
 * @property contentType String?
 * @property content String?
 */
class CreateReportSBMessage: ServiceBusMessage() {

    @SerializedName("upload_id")
    val uploadId: String? = null

    @SerializedName("destination_id")
    val destinationId: String? = null

    @SerializedName("event_type")
    val eventType: String? = null

    @SerializedName("stage_name")
    val stageName: String? = null

    @SerializedName("content_type")
    val contentType: String? = null

    val content: String? = null
}