package gov.cdc.ocio.processingstatusapi.model.traces

import com.google.gson.annotations.SerializedName

class Reference {
    @SerializedName("refType"   ) var refType   : String? = null
    @SerializedName("traceID"   ) var traceID   : String? = null
    @SerializedName("spanID"   ) var spanID   : String? = null
}