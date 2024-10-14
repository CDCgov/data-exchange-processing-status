package gov.cdc.ocio.rulesengine.interfaces

/**
 * The interface which defines the CRUD operations to perform on CosmosDB
 * using the CosmosRepository from commons-databasse library
 */

interface RulesRepository {

    /**
     * Create or update a rule in the database.
     * @param rule WorkflowRule The rule to be persisted.
     * @return The persisted WorkflowRule.
     */
    fun saveRule(rule: WorkflowRule): WorkflowRule

    /**
     * Fetch a rule from the database by its ID.
     * @param ruleId String The ID of the rule.
     * @return The WorkflowRule if found, or null if not.
     */
    fun findRuleById(ruleId: String): WorkflowRule?

    /**
     * Fetch all rules from the database.
     * @return List<WorkflowRule> A list of all rules.
     */
    fun findAllRules(): List<WorkflowRule>

    /**
     * Delete a rule by its ID.
     * @param ruleId String The ID of the rule to delete.
     * @return Boolean indicating whether the deletion was successful.
     */
    fun deleteRuleById(ruleId: String): Boolean
}

/*
     * Update an existing rule.
     * @param rule WorkflowRule The rule to be updated.
     * @return The updated WorkflowRule.
     /*
    fun updateRule(rule: WorkflowRule): WorkflowRule?
  */


}
*/