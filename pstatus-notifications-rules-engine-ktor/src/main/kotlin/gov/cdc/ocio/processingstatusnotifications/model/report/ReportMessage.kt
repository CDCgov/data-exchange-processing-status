package gov.cdc.ocio.processingstatusnotifications.model.report

import com.google.gson.annotations.SerializedName
import gov.cdc.ocio.messagesystem.models.DispositionType


/**
 * Incoming report message.
 *
 * @property uploadId String?
 * @property dataStreamId String?
 * @property dataStreamRoute String?
 * @property dispositionType DispositionType?
 * @property contentType String?
 * @property stageInfo StageInfo?
 */
class ReportMessage {

    @SerializedName("upload_id")
    val uploadId: String? = null

    @SerializedName("data_stream_id")
    val dataStreamId: String? = null

    @SerializedName("data_stream_route")
    val dataStreamRoute: String? = null

    @SerializedName("disposition_type")
    val dispositionType: DispositionType? = null

    @SerializedName("content_type")
    val contentType: String? = null

    @SerializedName("stage_info")
    val stageInfo: StageInfo? = null
}