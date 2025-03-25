package gov.cdc.ocio.processingstatusnotifications.model.report

import com.google.gson.annotations.SerializedName

enum class Status {

    @SerializedName("SUCCESS")
    SUCCESS,

    @SerializedName("FAILURE")
    FAILURE
}