package gov.cdc.ocio.processingstatusnotifications.model.report

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName
import java.time.Instant

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