package gov.cdc.ocio.processingstatusapi.model

import com.google.gson.annotations.SerializedName

class Spans {

    @SerializedName("traceID"       ) var traceID       : String?           = null
    @SerializedName("spanID"        ) var spanID        : String?           = null
    @SerializedName("operationName" ) var operationName : String?           = null
    @SerializedName("references"    ) var references    : ArrayList<Reference> = arrayListOf()
    @SerializedName("startTime"     ) var startTime     : String?              = null
    @SerializedName("duration"      ) var duration      : Int?              = null
    @SerializedName("tags"          ) var tags          : ArrayList<Tags>   = arrayListOf()
    @SerializedName("logs"          ) var logs          : ArrayList<String> = arrayListOf()
    @SerializedName("warnings"      ) var warnings      : ArrayList<String> = arrayListOf()
}