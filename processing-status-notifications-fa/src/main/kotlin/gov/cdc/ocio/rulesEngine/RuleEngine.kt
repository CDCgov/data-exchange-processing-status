package gov.cdc.ocio.rulesEngine

import gov.cdc.ocio.cache.InMemoryCacheService

object RuleEngine {
    private val rules = listOf(EmailNotificationRule(), WebsocketNotificationRule())
    private val cacheService = InMemoryCacheService()

    fun evaluateAllRules(ruleId: String): Boolean {
        run {
            for (rule in rules) {
                if (!rule.evaluate(ruleId, cacheService)) {
                    return false
                }
            }
        }
        return true
    }
}