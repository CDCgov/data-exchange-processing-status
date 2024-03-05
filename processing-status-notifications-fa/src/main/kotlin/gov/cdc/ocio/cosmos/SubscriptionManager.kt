package gov.cdc.ocio.cosmos

class SubscriptionManager {
    companion object {
        const val DEFAULT_RETRY_INTERVAL_MILLIS = 500L
        const val MAX_RETRY_ATTEMPTS = 100
        val subscriptionRuleContainerName = "SubscriptionRule"
        private val partitionKey = "/DestinationId"
        val subscriptionRuleContainer = CosmosContainerManager.initDatabaseContainer(subscriptionRuleContainerName, partitionKey)
    }
}