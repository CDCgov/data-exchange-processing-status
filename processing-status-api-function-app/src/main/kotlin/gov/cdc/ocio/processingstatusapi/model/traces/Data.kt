package gov.cdc.ocio.processingstatusapi.model.traces
import com.google.gson.annotations.SerializedName

class Data {

    @SerializedName("traceID"   ) var traceID   : String?          = null
    @SerializedName("spans"     ) var spans     : ArrayList<Spans> = arrayListOf()
    @SerializedName("processes" ) var processes : Processes?       = Processes()
    @SerializedName("warnings"  ) var warnings  : String?          = null
}