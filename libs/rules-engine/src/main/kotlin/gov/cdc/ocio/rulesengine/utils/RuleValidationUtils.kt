package gov.cdc.ocio.rulesengine.utils

import gov.cdc.ocio.rulesengine.exception.RuleValidationException
import gov.cdc.ocio.rulesengine.interfaces.WorkflowRule

/**
 * Utility class for validating rules before they are processed by the RuleEngine.
 */
object RuleValidationUtils {

    /**
     * Validates that the rule is not null and contains all required fields.
     * @param rule WorkflowRule The rule to be validated.
     * @throws RuleValidationException if the rule is invalid.
     */
    fun validateRule(rule: WorkflowRule) {
        if (rule.ruleId.isBlank()) {
            throw RuleValidationException("Rule ID cannot be blank.")
        }
        if (rule.ruleName.isBlank()) {
            throw RuleValidationException("Rule Name cannot be blank.")
        }
        if (rule.conditions.isEmpty()) {
            throw RuleValidationException("Rule must contain at least one condition.")
        }
        if (rule.actions.isEmpty()) {
            throw RuleValidationException("Rule must specify at least one action.")
        }
    }

    /**
     * Validates that the rule's conditions are valid.
     * @param rule WorkflowRule The rule containing conditions to be validated.
     * @throws RuleValidationException if any condition is invalid.
     */
    fun validateConditions(rule: WorkflowRule) {
        rule.conditions.forEach { (field, _) ->
            if (field.isBlank()) {
                throw RuleValidationException("Condition field cannot be blank.")
            }
        }
    }

    /**
     * Validates that the actions associated with a rule are valid.
     * @param rule WorkflowRule The rule containing actions to be validated.
     * @throws RuleValidationException if any action is invalid.
     */
    fun validateActions(rule: WorkflowRule) {
        rule.actions.forEach {
            if (it.name.isBlank()) {
                throw RuleValidationException("Action name cannot be blank.")
            }
            if (it.parameters.isEmpty()) {
                throw RuleValidationException("Action must contain at least one parameter.")
            }
        }
    }

    /** ~~~~FOR FUTURE USE~~~~~
     * Validates that the data provided for rule evaluation matches the rule's expected fields.
     * @param data Map<String, Any> The data to be evaluated by the rule.
     * @param rule WorkflowRule The rule that the data will be validated against.
     * @throws RuleValidationException if the data is invalid.
     */
    fun validateDataAgainstRule(data: Map<String, Any>, rule: WorkflowRule) {
        rule.conditions.forEach { (field, _) ->
            if (!data.containsKey(field)) {
                throw RuleValidationException("Data missing required field: $field")
            }
        }
    }
}
