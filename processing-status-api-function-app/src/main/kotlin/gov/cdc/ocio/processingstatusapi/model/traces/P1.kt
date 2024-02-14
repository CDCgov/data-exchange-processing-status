package gov.cdc.ocio.processingstatusapi.model.traces

import com.google.gson.annotations.SerializedName

class P1 {
    @SerializedName("serviceName" ) var serviceName : String?         = null
    @SerializedName("tags"        ) var tags        : ArrayList<Tags> = arrayListOf()
}