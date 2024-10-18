package gov.cdc.ocio.rulesengine.implementation

import gov.cdc.ocio.rulesengine.exception.RuleNotFoundException
import gov.cdc.ocio.rulesengine.exception.RulesEngineException
import gov.cdc.ocio.rulesengine.interfaces.RulesEngine
import gov.cdc.ocio.rulesengine.interfaces.WorkflowRule
import gov.cdc.ocio.rulesengine.repository.CosmosRuleRepository
import gov.cdc.ocio.rulesengine.utils.RuleValidationUtils
import jdk.jshell.spi.ExecutionControl.NotImplementedException
import mu.KLogger
import kotlin.jvm.Throws

/**
 * The default implementation class which gets exposed to the other apps and libs and which abstracts the
 * cosmos db CRUD operations
 * This uses the persistence layer the CosmosRuleRepository to perform the CRUD
 */
class WorkflowRulesEngine(
    private val repository: CosmosRuleRepository,
    private val logger: KLogger
) : RulesEngine {

    /**
     * The function which adds a rule ot the Cosmos Db
     * @param rule WorkflowRule
     * @return String - the ruleId
     */
    @Throws(RulesEngineException::class)
    override fun addRule(rule: WorkflowRule): String {
        return try {
            // Validate the rule before adding it to the system
            RuleValidationUtils.validateRule(rule)
            RuleValidationUtils.validateConditions(rule)
            RuleValidationUtils.validateActions(rule)

            val workflowRule = repository.saveRule(rule)
            val ruleId = workflowRule.ruleId
            logger.info("Rule added with Id: $ruleId")
            ruleId

        } catch (ex: RulesEngineException) {
            logger.error("Error adding rule: ${ex.message}")
            throw ex
        }
    }
    /**
     * The function which deletes  a rule by ruleId
     * @param ruleId String
     * @return Boolean
     */
    @Throws(RulesEngineException::class)
    override fun deleteRule(ruleId: String): Boolean {
        return try {
            val result = repository.deleteRuleById(ruleId)
            logger.info("Rule deleted: $ruleId")
            result

        } catch (ex: RulesEngineException) {
            logger.error("Error deleting rule: ${ex.message}")
            throw ex
        }
    }
    /**
     * The function which deletes a rule by ruleId
     * @param ruleId String
     * @return Boolean
     */
    @Throws(RuleNotFoundException::class)
    override fun getRule(ruleId: String): WorkflowRule? {
        return try {
            val result = repository.findRuleById(ruleId)
            result
        } catch (ex: RuleNotFoundException) {
            logger.error("Error retrieving rule: ${ex.message}")
            throw ex
        }
    }
    /**
     * The function which gets all rules
     * @return List<WorkflowRule>
     */
    @Throws(RulesEngineException::class)
    override fun getRules(): List<WorkflowRule?> {
        return try {
            val results=  repository.findAllRules()
            results
        } catch (ex: RulesEngineException) {
            logger.error("Error retrieving all rules: ${ex.message}")
            throw ex
        }
    }
    override fun updateRule(updatedRule: WorkflowRule): WorkflowRule? {
        throw NotImplementedException("This function has not yet been implemented")
    }
    override fun evaluateRule(ruleId: String, data: Map<String, Any>): Boolean  {
        throw NotImplementedException("This function has not yet been implemented")
    }

    /* TODO - ***FUTURE USE*** To Check with Matt on whether these are required
     override fun updateRule(updatedRule: WorkflowRule): WorkflowRule? {
        return try {
            // Validate the rule before adding it to the system
            RuleValidationUtils.validateRule(updatedRule)
            RuleValidationUtils.validateConditions(updatedRule)
            RuleValidationUtils.validateActions(updatedRule)

           // val result = repository.updateRule(updatedRule)
          //  logger.info("Rule updated: $result.id")
            //result
            null
        } catch (ex: Exception) {
            logger.error("Error updating rule: ${ex.message}")
            throw RulesEngineException("Failed to update rule", ex)
        }
    }
     */
    /* override fun evaluateRule(ruleId: String, data: Map<String, Any>): Boolean {
        /* val rule = getRule(ruleId) ?: throw RulesEngineException("Rule not found for ID: $ruleId")
         return rule.evaluate(data)
         // Validate the data against the rule before evaluating
         RuleValidationUtils.validateDataAgainstRule(data, rule)*/
         throw NotImplementedException("Not implemented")
     } */
}
