package gov.cdc.ocio.model.cache

data class SubscriptionRule(val destinationId: String,
                       val eventType: String,
                       val stageName: String,
                       val statusType: String) {

    fun getStringHash(): String {
        return Integer.toHexString(hashCode())
    }
}