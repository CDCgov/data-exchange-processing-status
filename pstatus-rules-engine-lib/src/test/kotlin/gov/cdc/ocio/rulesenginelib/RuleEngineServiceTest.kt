package gov.cdc.ocio.rulesenginelib
import gov.cdc.ocio.rulesenginelib.gov.cdc.ocio.rulesenginelib.cosmos.CosmosDBService
import org.testng.annotations.BeforeTest
import org.testng.annotations.Test
import org.testng.Assert.*
import java.util.*

class RuleEngineServiceTest {

    private lateinit var cosmosDBService: CosmosDBService
    private lateinit var ruleEngineService: RuleEngineService

    private val uri= System.getProperty("uri")
    private val containerName = System.getProperty("containerName")
    private val partitionKey = System.getProperty("partitionKey")
    private val authkey =  System.getProperty("authKey")
    private var ruleId:String?= ""
    private var workflowId:String?= ""
    private val condition:String="upload from all jurisdictions should occur by 12pm"

    @BeforeTest
    fun setUp() {
        cosmosDBService = CosmosDBService(uri, authkey, containerName, partitionKey)
        ruleEngineService = RuleEngineService(cosmosDBService)
    }

    @Test(priority = 1)
    fun `saveRule should save a rule successfully`() {
        // Arrange
        val response=  ruleEngineService.addEasyRule(condition)
        ruleId= response.ruleId
        assertNotNull(response.ruleId)
    }

    @Test(priority = 2)
    fun `getRuleById should return the correct rule`() {
        val response=  ruleEngineService.getRuleById(ruleId!!)
        assertNotNull(response.ruleId)
        assertEquals(response.ruleId, ruleId)
    }

    @Test(priority = 3)
    fun `getRuleByCondition should return the correct rule`() {
        val response=  ruleEngineService.getRuleByCondition(ruleId!!, condition)
        assertNotNull(response.ruleId)
        assertEquals(response.ruleId, ruleId)
        assertEquals(response.condition, condition)
    }
    @Test(priority = 4)
    fun `saveRule for webhooks should return the correct rule`() {
        workflowId = UUID.randomUUID().toString()
        val response=  ruleEngineService.addWorkflowWebhookRule(workflowId!!,null,"http://example.com")
        ruleId= response.ruleId
        assertNotNull(response.ruleId)
        assertEquals(response.workflowId, workflowId)
    }

    @Test(priority = 5)
    fun `get rule by workflowId`() {
       val response=  ruleEngineService.getRuleByWorkflowId(workflowId!!)
        assertEquals(response.workflowId, workflowId)
        assertNotNull(response.ruleId)
    }

    @Test(priority = 6)
    fun `saveRule for email should return the correct rule`() {
        val workflowId = UUID.randomUUID().toString()
        val response=  ruleEngineService.addWorkflowEmailRule(workflowId,null, listOf("abc@example.com", "xyz@example.com"))
        ruleId= response.ruleId
        assertNotNull(response.ruleId)
    }

    @Test(priority = 7)
    fun `get Rules by Rule Type Easy Rules`() {
       val responses=  ruleEngineService.getAllByRuleType(RuleType.EASY_RULES)
        assertNotNull(responses)
        assertTrue(responses.count() > 1)
    }

    @Test(priority = 8)
    fun `get Rules by Rule Type Workflow`() {
        val responses=  ruleEngineService.getAllByRuleType(RuleType.WORKFLOW)
        assertNotNull(responses)
        assertTrue(responses.count() > 1)
    }

    /*@Test
    fun `saveRule should throw exception after max attempts`() {
        // Arrange
        val rule = Rule("ruleId1", "sampleRule")
        every {
            mockContainer.createItem(
                CharMatcher.any(),
                PartitionKey(CharMatcher.any()),
                CosmosItemRequestOptions()
            )
        } throws RuntimeException("Simulated exception")

        // Act & Assert
        assertThrows {
            cosmosDBService.saveRule(rule)
        }

        // Verify that createItem was called MAX_RETRY_ATTEMPTS times
        verify(exactly = CosmosDBService.MAX_RETRY_ATTEMPTS) {
            mockContainer.createItem(CharMatcher.any(), PartitionKey(CharMatcher.any()), CosmosItemRequestOptions())
        }
    }*/
}
