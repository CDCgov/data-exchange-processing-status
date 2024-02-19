package gov.cdc.ocio.rulesEngine


interface Rule {
    fun evaluate(ruleId: String): Boolean
}