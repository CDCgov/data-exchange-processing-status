package gov.cdc.ocio.rulesenginelib.gov.cdc.ocio.rulesenginelib.cosmos

import com.azure.cosmos.models.CosmosItemRequestOptions
import com.azure.cosmos.models.CosmosQueryRequestOptions
import com.azure.cosmos.models.PartitionKey
import gov.cdc.ocio.rulesenginelib.Rule
import gov.cdc.ocio.rulesenginelib.RuleType
import io.netty.handler.codec.http.HttpResponseStatus
import mu.KotlinLogging
import org.koin.core.component.KoinComponent

/**
 * This class will handle the interaction with CosmosDB. Use the Azure CosmosDB SDK to handle the connection and data writing.
 * @param endpoint String
 * @param key String
 *
 */

class CosmosDBService(private val endpoint: String, private val key: String,
                      private val containerName:String, private val partitionKey:String):KoinComponent {


    private val logger = KotlinLogging.logger {}

    private val container = CosmosContainerManager.initDatabaseContainer(endpoint, key, containerName, partitionKey)
    /**
     * The function which writes the rule to CosmosDB
     * @param rule Rule
     * @return responseRuleId String
     */

    // Save a rule to CosmosDB
    fun saveRule(rule: Rule):String {
        var attempts = 0
        var isValidResponse: Boolean
        var recommendedDuration: String? = null
        var responseRuleId = ""
        var statusCode: Int? = null
        val typeName = "rule"

        do {
            try {

                val response = container?.createItem(
                    rule,
                    PartitionKey(rule.ruleId),
                    CosmosItemRequestOptions()
                )
                isValidResponse = response != null
                if (response?.item is Rule) {
                    responseRuleId = response.item?.ruleId ?: "0"
                    statusCode = response.statusCode
                    recommendedDuration = response.responseHeaders?.get("x-ms-retry-after-ms")
                }
                logger.info("Creating ${typeName}, response http status code = ${statusCode}, attempt = ${attempts + 1}")
                if (isValidResponse) {

                    when (statusCode) {
                        HttpResponseStatus.OK.code(), HttpResponseStatus.CREATED.code() -> {
                            logger.info("Created rule with Id = ${responseRuleId}")
                            return responseRuleId
                        }

                        HttpResponseStatus.TOO_MANY_REQUESTS.code() -> {
                            logger.warn("Received 429 (too many requests) from cosmosdb, attempt ${attempts + 1}, will retry after $recommendedDuration millis")
                            val waitMillis = recommendedDuration?.toLong()
                            Thread.sleep(waitMillis ?: DEFAULT_RETRY_INTERVAL_MILLIS)
                        }

                        else -> {
                            // Need to retry regardless
                            val retryAfterDurationMillis = getCalculatedRetryDuration(attempts)
                            logger.warn("Received response code ${statusCode}, attempt ${attempts + 1}, will retry after $retryAfterDurationMillis millis")
                            Thread.sleep(retryAfterDurationMillis)
                        }
                    }
                } else {
                    val retryAfterDurationMillis = getCalculatedRetryDuration(attempts)
                    logger.warn("Received null response from cosmosdb, attempt ${attempts + 1}, will retry after $retryAfterDurationMillis millis")
                    Thread.sleep(retryAfterDurationMillis)
                }
            }
            catch(e: Exception) {
                val retryAfterDurationMillis = getCalculatedRetryDuration(attempts)
                logger.error("CreateReport: Exception: ${e.localizedMessage}, attempt ${attempts + 1}, will retry after $retryAfterDurationMillis millis")
                Thread.sleep(retryAfterDurationMillis)
            }
        } while (attempts++ < MAX_RETRY_ATTEMPTS)

        throw Exception("Failed to create rule with Id = $responseRuleId")
    }

    /**
     * The function to get the rule by Id
     * @param ruleId String
     * @return Rule
     */

    fun getRuleById(ruleId:String) : Rule {
        val sqlQuery = "select * from r where r.ruleId = '$ruleId'"
        val items = container?.queryItems(
            sqlQuery, CosmosQueryRequestOptions(),
            Rule::class.java
        )
        if ((items?.count() ?: 0) == 0) {
            logger.info("No Rule found with Id= $ruleId")
        }
        return items?.first()!!
    }

    /**
     * The function to get the rule by condition and Id. This would be mainly for retrieving EasyRules related rules
     * @param ruleId String
     * @param condition String
     * @return Rule
     */

    fun getRuleByCondition(ruleId:String, condition:String) : Rule {
        val sqlQuery = "select * from r where r.ruleId = '$ruleId' and r.condition ='$condition'"
        val items = container?.queryItems(
            sqlQuery, CosmosQueryRequestOptions(),
            Rule::class.java
        )
        if ((items?.count() ?: 0) == 0) {
            logger.info("No Rule found with Id= $ruleId and condition= $condition")
        }
        return items?.first()!!
    }
    /**
     * The function to get the rule by workflowId for temporal workflow rules
     * @param workflowId String
     * @return Rule
     */

    fun getRuleByWorkflowId(workflowId:String) : Rule {
        val sqlQuery = "select * from r where r.workflowId='$workflowId'"
        val items = container?.queryItems(
            sqlQuery, CosmosQueryRequestOptions(),
            Rule::class.java
        )
        if ((items?.count() ?: 0) == 0) {
            logger.info("No Rule found for workflow Id= $workflowId")
        }
        return items?.first()!!
    }

      /**
     * The function to get all rules of type Workflow or Easy rules
       * @param ruleType RuleType
     * @return List<Rule>
     */

    fun getAllByRuleType(ruleType:RuleType) : List<Rule> {
        val sqlQuery = "select * from r where r.type='${ruleType}'"
        val items = container?.queryItems(
            sqlQuery, CosmosQueryRequestOptions(),
            Rule::class.java
        )
        if ((items?.count() ?: 0) == 0) {
            logger.info("No Rules found for type Workflow")
        }
        return items!!.toList()
    }

    /**
     * The function which calculates the interval after which the retry should occur
     * @param attempt Int
     */
    private fun getCalculatedRetryDuration(attempt: Int): Long {
        return DEFAULT_RETRY_INTERVAL_MILLIS * (attempt + 1)
    }
    companion object {
        const val DEFAULT_RETRY_INTERVAL_MILLIS = 500L
        const val MAX_RETRY_ATTEMPTS = 100
    }
}
