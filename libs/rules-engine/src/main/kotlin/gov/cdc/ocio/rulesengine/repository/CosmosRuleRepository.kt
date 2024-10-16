package gov.cdc.ocio.rulesengine.repository

import gov.cdc.ocio.database.cosmos.CosmosRepository
import gov.cdc.ocio.rulesengine.exception.RuleNotFoundException
import gov.cdc.ocio.rulesengine.exception.RulesEngineException
import gov.cdc.ocio.rulesengine.interfaces.RulesRepository
import gov.cdc.ocio.rulesengine.interfaces.WorkflowRule
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import java.util.*

/**
 * The class which implements the interface RulesRepository and uses the Cosmos Repository from commons-database lib
 * to persist rules, conditions, and actions.
 * @param uri String
 * @param authKey String
 * @param containerName String
 */
class CosmosRuleRepository(
    private val uri: String,
    private val authKey: String,
    private val containerName:String
) :  KoinComponent,RulesRepository {
    //private val repository by inject<ProcessingStatusRepository>()
    private val repository: CosmosRepository by inject { parametersOf(uri, authKey,"/ruleId", containerName) }
    private val logger = KotlinLogging.logger {}
    /**
     * The function which saves the rule to the Cosmos SB container
     * @param rule WorkflowRule
     * @return WorkflowRule
     */
    override fun saveRule(rule: WorkflowRule): WorkflowRule {
        return try {

            repository.rulesCollection.createItem(
                rule.id,
                rule,
                WorkflowRule::class.java,
                rule.ruleId)
            logger.info("Successfully saved rule with ID: ${rule.ruleId}")
            rule

        } catch (e: Exception) {
            logger.error("Failed to save rule: ${rule.ruleId}", e)
            throw RulesEngineException("Failed to save the rule",e)
        }
    }
    /**
     * The function which retrieves a rule by ruleId
     * @param ruleId String
     * @return WorkflowRule
     */
    override fun findRuleById(ruleId: String): WorkflowRule? {
        return try {
            val cName = repository.rulesCollection.collectionNameForQuery
            val cVar = repository.rulesCollection.collectionVariable
            val cPrefix = repository.rulesCollection.collectionVariablePrefix
            val sqlQuery = (
                    "select * from $cName $cVar "
                            + "where ${cPrefix}ruleId = '$ruleId' ")
            val items = repository.rulesCollection.queryItems(
                sqlQuery,
                WorkflowRule::class.java
            )
            if(items.any()){
                items.first()
            }
            else
                null
        } catch (e: Exception) {
            logger.error("Rule with ID $ruleId not found OR Error fetching rule with ID: $ruleId", e)
            throw RuleNotFoundException("Rule with id $ruleId not found in the container")

        }
    }
    /**
     * The function which retrieves all the rules under the Rules container
     * @return  List<WorkflowRule>
     */
    override fun findAllRules(): List<WorkflowRule> {
        try {
            val cName = repository.rulesCollection.collectionNameForQuery
            val cVar = repository.rulesCollection.collectionVariable
            val sqlQuery = ("select * from $cName $cVar ")
            val items = repository.rulesCollection.queryItems(
                sqlQuery,
                WorkflowRule::class.java
            )
            return items
        } catch (e: Exception) {
            logger.error("Failed to fetch all rules", e)
            throw RulesEngineException("Failed to fetch all rules from the rules container", e)
        }
    }
    /**
     * The function which deletes a rule by ruleId
     * @param ruleId String
     * @return  Boolean
     */
    override fun deleteRuleById(ruleId: String): Boolean {
        return try {
            repository.rulesCollection.deleteItem(
                ruleId,
                ruleId
            )
            true
        } catch (e: Exception ) {
            logger.error("Rule with rule Id $ruleId not found for deletion or failed to delete rule with ID: $ruleId", e)
            throw RulesEngineException("Failed to delete the rule using ruleId $ruleId", e)
        }
    }

    /**
     * Update an existing WorkflowRule.
     * This method first checks if the rule exists in Cosmos DB. If it exists, it replaces the item.
     * @param rule WorkflowRule The rule to be updated.
     * @return The updated WorkflowRule.
     *//*
    override fun updateRule(rule: WorkflowRule): WorkflowRule {
        return try {
            val existingRule = findRuleById(rule.id)
            if (existingRule != null) {
                val response = container.replaceItem(rule, rule.id, PartitionKey(rule.id))
                logger.info("Successfully updated rule with ID: ${rule.id}")
                response.item
            } else {
                logger.warn("Rule with ID: ${rule.id} not found for update")
                throw RuleEngineException("Rule with ID: ${rule.id} not found for update")
            }
        } catch (e: CosmosException) {
            logger.error("Failed to update rule: ${rule.id}", e)
            throw RuleEngineException("Failed to update rule in Cosmos DB", e)
        }
    }*/
}
