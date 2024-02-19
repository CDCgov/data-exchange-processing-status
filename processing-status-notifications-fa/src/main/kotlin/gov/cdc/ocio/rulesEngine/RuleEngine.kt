package gov.cdc.ocio.rulesEngine

object RuleEngine {

    private val rules = listOf(EmailNotificationRule(), WebsocketNotificationRule())

    fun evaluateAllRules(ruleId: String): Boolean {
        for (rule in rules) {
            if (!rule.evaluate(ruleId)) {
                return false
            }
        }
        return true
    }
}