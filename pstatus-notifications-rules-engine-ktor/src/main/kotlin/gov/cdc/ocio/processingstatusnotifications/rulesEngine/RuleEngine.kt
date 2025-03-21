package gov.cdc.ocio.processingstatusnotifications.rulesEngine

import gov.cdc.ocio.processingstatusnotifications.cache.InMemoryCacheService

/**
 * Manages the rules engine
 */
object RuleEngine {

    private val rules = listOf(
        EmailNotificationRule(),
        WebsocketNotificationRule()
    )

    private val cacheService = InMemoryCacheService()

    fun evaluateAllRules(ruleId: String): List<String> {
        val dispatchMsgsForTesting = mutableListOf<String>()
        for (rule in rules) dispatchMsgsForTesting += rule.evaluateAndDispatch(ruleId, cacheService)
        return dispatchMsgsForTesting
    }
}