package gov.cdc.ocio.processingstatusnotifications.model.cache

import gov.cdc.ocio.processingstatusnotifications.model.message.Status


class SubscriptionRule(
    val dataStreamId: String,
    val dataStreamRoute: String,
    val service: String,
    val action: String,
    val status: Status
) {
    override fun hashCode(): Int {
        var result = dataStreamId.lowercase().hashCode()
        result = 31 * result + dataStreamRoute.lowercase().hashCode()
        result = 31 * result + service.lowercase().hashCode()
        result = 31 * result + action.lowercase().hashCode()
        result = 31 * result + status.name.hashCode()
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
        if (service != other.service) return false
        if (action != other.action) return false
        if (status != other.status) return false

        return true
    }
}