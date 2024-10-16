package gov.cdc.ocio.rulesengine.interfaces

import gov.cdc.ocio.rulesengine.models.RuleAction

/**
 * The interface which defines the work flow rule attributes
 *
 */
interface WorkflowRule {
    val id: String // surrogate key
    val ruleId:String //partition key
    val ruleName:String
    val conditions: Map<String, Any> // Condition map defining the rule logic
    val state: String // ***For future use*** - State associated with this rule, e.g., "ACTIVE", "INACTIVE"
    val actions: List<RuleAction> // List of actions to be taken when conditions are met
    fun evaluate(data: Map<String, Any>): Boolean // Logic for evaluating conditions
}
