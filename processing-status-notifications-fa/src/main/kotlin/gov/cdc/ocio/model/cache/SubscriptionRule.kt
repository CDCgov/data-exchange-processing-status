package gov.cdc.ocio.model.cache

class SubscriptionRule(val destinationId: String,
                       val eventType: String,
                       val stageName: String,
                       val statusType: String) {

    override fun hashCode(): Int {
        var result = destinationId.lowercase().hashCode()
        result = 31 * result + eventType.lowercase().hashCode()
        result = 31 * result + stageName.lowercase().hashCode()
        result = 31 * result + statusType.lowercase().hashCode()
        return result
    }

    fun getStringHash(): String {
        return Integer.toHexString(hashCode())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SubscriptionRule

        if (destinationId != other.destinationId) return false
        if (eventType != other.eventType) return false
        if (stageName != other.stageName) return false
        if (statusType != other.statusType) return false

        return true
    }
}