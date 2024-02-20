package gov.cdc.ocio.rulesEngine

import gov.cdc.ocio.cache.InMemoryCacheService

object RuleEngine {
    private val rules = listOf(EmailNotificationRule(), WebsocketNotificationRule())
    private val cacheService = InMemoryCacheService()

    fun evaluateAllRules(ruleId: String): List<String> {
        val dispatchMsgsForTesting = mutableListOf<String>()
        for (rule in rules) dispatchMsgsForTesting += rule.evaluateAndDispatch(ruleId, cacheService)
        return dispatchMsgsForTesting
    }
}