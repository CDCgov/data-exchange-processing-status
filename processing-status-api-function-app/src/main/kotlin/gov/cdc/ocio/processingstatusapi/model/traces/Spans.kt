package gov.cdc.ocio.processingstatusapi.model.traces

import com.google.gson.annotations.SerializedName

class Spans {

    @SerializedName("traceID"       ) var traceID       : String?           = null
    @SerializedName("spanID"        ) var spanID        : String?           = null
    @SerializedName("operationName" ) var operationName : String?           = null
    @SerializedName("references"    ) var references    : ArrayList<Reference> = arrayListOf()
    @SerializedName("startTime"     ) var startTime     : Long?             = null
    @SerializedName("duration"      ) var duration      : Long?             = null
    @SerializedName("tags"          ) var tags          : ArrayList<Tags>   = arrayListOf()
    @SerializedName("logs"          ) var logs          : ArrayList<String> = arrayListOf()
    @SerializedName("warnings"      ) var warnings      : ArrayList<String> = arrayListOf()
}