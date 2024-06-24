package gov.cdc.ocio.processingstatusnotifications.model.cache

class SubscriptionRule(val dataStreamId: String,
                       val dataStreamRoute: String,
                       val stageName: String,
                       val statusType: String) {

    override fun hashCode(): Int {
        var result = dataStreamId.lowercase().hashCode()
        result = 31 * result + dataStreamRoute.lowercase().hashCode()
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

        if (dataStreamId != other.dataStreamId) return false
        if (dataStreamRoute != other.dataStreamRoute) return false
        if (stageName != other.stageName) return false
        if (statusType != other.statusType) return false

        return true
    }
}