package gov.cdc.ocio.database.models

import com.google.gson.annotations.SerializedName

/*
   Status of whether the report is a SUCCESS OR FAILURE
 */
enum class Status {

    @SerializedName("SUCCESS")
    SUCCESS,

    @SerializedName("FAILURE")
    FAILURE
}