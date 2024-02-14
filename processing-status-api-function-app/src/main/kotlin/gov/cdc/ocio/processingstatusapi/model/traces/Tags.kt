package gov.cdc.ocio.processingstatusapi.model.traces

import com.google.gson.annotations.SerializedName

class Tags {

    @SerializedName("key"   ) var key   : String? = null
    //@SerializedName("type"  ) var type  : String? = null
    @SerializedName("value" ) var value : String? = null
}