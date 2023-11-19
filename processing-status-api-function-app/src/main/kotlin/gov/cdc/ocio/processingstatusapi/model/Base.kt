package gov.cdc.ocio.processingstatusapi.model
import com.google.gson.annotations.SerializedName

class Base {
    @SerializedName("data"   ) var data   : ArrayList<Data> = arrayListOf()
    @SerializedName("total"  ) var total  : Int?            = null
    @SerializedName("limit"  ) var limit  : Int?            = null
    @SerializedName("offset" ) var offset : Int?            = null
    @SerializedName("errors" ) var errors : String?         = null

}