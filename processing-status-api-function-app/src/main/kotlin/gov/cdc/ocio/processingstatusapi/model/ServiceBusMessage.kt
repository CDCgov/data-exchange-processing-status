package gov.cdc.ocio.processingstatusapi.model

import com.google.gson.annotations.SerializedName

enum class RequestType {

    UNDEFINED,

    @SerializedName("create")
    CREATE,

    @SerializedName("amend")
    AMEND
}

open class ServiceBusMessage {

    var requestType = RequestType.UNDEFINED
}