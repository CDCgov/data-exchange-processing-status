package gov.cdc.ocio.model.http

import java.util.*

class SubscriptionResult {
    var subscription_id: UUID? = null
    var timestamp: Long? = null
    var status: Boolean? = false
    var message: String? = ""
}