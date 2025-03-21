package gov.cdc.ocio.processingstatusnotifications.model.message

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName
import gov.cdc.ocio.messagesystem.models.DispositionType
import java.time.Instant


enum class Status {

    @SerializedName("SUCCESS")
    SUCCESS,

    @SerializedName("FAILURE")
    FAILURE
}

/**
 * Issue level of Report-ERROR OR WARNING
 */
enum class Level {
    @SerializedName("ERROR")
    ERROR,
    @SerializedName("WARNING")
    WARNING
}

class Issue {

    var level : Level? = null

    var message: String? = null
}

class StageInfo {

    var service : String? = null

    var action: String? = null

    var version: String? = null

    var status: Status? = null

    var issues: List<Issue>? = null

    @SerializedName("start_processing_time")
    @JsonProperty("start_processing_time")
    var startProcessingTime: Instant? = null

    @SerializedName("end_processing_time")
    @JsonProperty("end_processing_time")
    var endProcessingTime: Instant? = null
}

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