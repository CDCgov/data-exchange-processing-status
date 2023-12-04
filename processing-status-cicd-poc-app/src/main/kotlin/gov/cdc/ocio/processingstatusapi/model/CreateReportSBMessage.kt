package gov.cdc.ocio.processingstatusapi.model

import com.google.gson.annotations.SerializedName

/**
 * Create a report service bus message.
 *
 * @property uploadId String?
 * @property destinationId String?
 * @property eventType String?
 */
class CreateReportSBMessage: ServiceBusMessage() {

    @SerializedName("upload_id")
    val uploadId: String? = null

    @SerializedName("destination_id")
    val destinationId: String? = null

    @SerializedName("event_type")
    val eventType: String? = null
}