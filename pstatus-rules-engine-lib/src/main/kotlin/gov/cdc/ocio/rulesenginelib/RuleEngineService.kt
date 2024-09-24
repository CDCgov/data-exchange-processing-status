package gov.cdc.ocio.rulesenginelib

import gov.cdc.ocio.rulesenginelib.gov.cdc.ocio.rulesenginelib.cosmos.CosmosDBService
import java.util.*

/**
 * This is the service that other Ktor projects will interact with to create or persist rules.
 * @param cosmosService CosmosDBService
 * @return Rule
 */
class RuleEngineService(private val cosmosService: CosmosDBService) {

    // Method to add an EasyRule type rule
    fun addEasyRule(condition: String):Rule {
        val id = UUID.randomUUID().toString()
        val rule = Rule(
            id=id,
            ruleId = id,
            type = RuleType.EASY_RULES,
            condition = condition
        )
        cosmosService.saveRule(rule)
        return rule
    }

    /**
     * The function to add a Workflow rule with Email notifications for temporal rules engine
     * * @param workflowId : String
     *   @param condition : String?
     *  @param emails List<String>
     *  @return Rule
     */

    fun addWorkflowEmailRule(workflowId:String?,condition: String?, emails: List<String>):Rule {
        val id = UUID.randomUUID().toString()
        val rule = Rule(
            id=id,
            ruleId = id,
            workflowId = workflowId,
            type = RuleType.WORKFLOW,
            condition = condition,
            emailNotification = EmailNotification(emails)
        )
        cosmosService.saveRule(rule)
        return rule
    }
    /**
     * The function to add a Workflow rule with Webhook notifications
     * @param workflowId : String
     * @param condition : String?
     * @param webhookUrl String
     *  @return Rule
     */

    fun addWorkflowWebhookRule(workflowId:String, condition: String?, webhookUrl: String):Rule {
        val id = UUID.randomUUID().toString()
        val rule = Rule(
            id=id,
            ruleId = id,
            workflowId= workflowId,
            type = RuleType.WORKFLOW,
            condition = condition,
            webhookNotification = WebhookNotification(webhookUrl)
        )
        cosmosService.saveRule(rule)
        return rule
    }

    /**
     * The function which returns rule by Id
     * @param ruleId String
     * @return Rule
     */

    fun getRuleById(ruleId: String) :Rule{
        return cosmosService.getRuleById(ruleId)
    }

    /**
     * The function which returns rule by condition andby Id
     * @param ruleId String
     * @param condition String
     * @return Rule
     */
    fun getRuleByCondition(ruleId: String, condition:String) :Rule{
        return cosmosService.getRuleByCondition(ruleId, condition)
    }
    /**
     * The function which returns rule for a specific workflow id
     * @param workflowId String
     * @return Rule
     */
    fun getRuleByWorkflowId(workflowId: String) :Rule{
        return cosmosService.getRuleByWorkflowId(workflowId)
    }
    /**
     * The function which returns rules for a specific rule type
     * @param ruleType RuleType
     * @return listOf Rule
     */
    fun getAllByRuleType(ruleType: RuleType) :List<Rule>{
        return cosmosService.getAllByRuleType(ruleType)
    }
}
