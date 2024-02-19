package gov.cdc.ocio.rulesEngine

class EmailNotificationRule(): Rule {
    override fun evaluate(ruleId: String): Boolean {
        println("Email Rule Matched")
        return true;
    }
}