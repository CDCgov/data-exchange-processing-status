package gov.cdc.ocio.processingstatusapi.models.reports

import com.google.gson.annotations.SerializedName
import java.util.*

/*
   Status of whether the report is a SUCCESS OR FAILURE
 */
enum class Status {

    @SerializedName("SUCCESS")
    SUCCESS,

    @SerializedName("FAILURE")
    FAILURE
}

/**
 * Get StageInfo from the report message.
 *
 * @property service String?
 * @property action String?
 * @property version String?
 * @property status String?
 * @property startProcessingTime Date?
 * @property endProcessingTime Date?
 */
class StageInfo {

    @SerializedName("service")
    var service : String? = null

    @SerializedName("action")
    var action: String? = null

    @SerializedName("version")
    var version: String? = null

    @SerializedName("status")
    var status: Status? = null

    @SerializedName("issues")
    var issues: List<Issue>? = null

    @SerializedName("start_processing_time")
    var startProcessingTime: Date? = null

    @SerializedName("end_processing_time")
    var endProcessingTime: Date? = null


}