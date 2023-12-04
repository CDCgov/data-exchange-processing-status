package gov.cdc.ocio.processingstatusapi.model

import com.google.gson.annotations.SerializedName

/**
 * Amend an existing report service bus message.
 *
 * @property uploadId String?
 * @property stageName String?
 * @property contentType String?
 * @property content String?
 */
class AmendReportSBMessage: ServiceBusMessage() {

    @SerializedName("upload_id")
    val uploadId: String? = null

    @SerializedName("stage_name")
    val stageName: String? = null

    @SerializedName("content_type")
    val contentType: String? = null

    val content: String? = null
}