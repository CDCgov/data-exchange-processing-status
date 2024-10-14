package gov.cdc.ocio.rulesengine.interfaces

import gov.cdc.ocio.rulesengine.exception.RulesEngineException

/** The core RulesEngine interface for managing rules (CRUD operations).
 *
 */

interface RulesEngine {

    @Throws(RulesEngineException::class)
    fun addRule(rule: WorkflowRule): String // Returns the rule ID

    @Throws(RulesEngineException::class)
    fun deleteRule(ruleId: String): Boolean // True if successful, false otherwise

    @Throws(RulesEngineException::class)
    fun updateRule(updatedRule: WorkflowRule): WorkflowRule?

    @Throws(RulesEngineException::class)
    fun getRule(ruleId: String): WorkflowRule?

    @Throws(RulesEngineException::class)
    fun getRules(): List<WorkflowRule?>

    @Throws(RulesEngineException::class)
    fun evaluateRule(ruleId: String, data: Map<String, Any>): Boolean // Evaluate rule against some data
}
