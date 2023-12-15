package gov.cdc.ocio.processingstatusapi.model.traces

import com.google.gson.annotations.SerializedName
import gov.cdc.ocio.processingstatusapi.model.traces.P1

class Processes {
    @SerializedName("p1" ) var p1 : P1? = P1()
}