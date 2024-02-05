package gov.cdc.ocio.model.http

import com.google.gson.annotations.SerializedName
import java.util.*

class SubscriptionResult {
    var subscription_id: String? = null
    var timestamp: Long? = null
    var status: Boolean? = false
    var message: String? = ""
}

enum class StatusType {
    SUCCESS,
    WARNING,
    FAILURE
}

enum class SubscriptionType {
    EMAIL,
    WEBSOCKET,
}