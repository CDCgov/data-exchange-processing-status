# rules-engine
The `rules-engine` library allows business rules to be dynamically defined to drive actions.  The intended purpose is for use in workflows to specify conditions for state determination.

# Structure Overview  

## Interfaces:

### RulesEngine: 
Core RulesEngine interface for managing rules (CRUD operations).
### RulesRepository: 
Interface which defines the CRUD operations to perform on CosmosDB using the CosmosRepository from commons-database library
### WorkflowRule: 
Interface which defines the work flow rule attributes.

## Models

### Rule Action
The class which defines the RuleAction attributes

## Repository

## CosmosRuleRepository
The class which implements the interface RulesRepository and uses the Cosmos Repository from commons-database lib
to persist rules, conditions, and actions.

## Implementation class

### WorkflowRulesEngine: 
The default implementation class which gets exposed to the other apps and libs and which abstracts the
cosmos db CRUD operations.This uses the persistence layer the CosmosRuleRepository to perform the CRUD

### RuleValidationUtils: 
Utility class for validating rules before they are processed by the RuleEngine.

## Exception

### RuleNotFoundException
The exception thrown when a rule is not found in Cosmos Db
### RulesEngineException
The default  exception which gets thrown during the CRUD operations

### RuleValidationException
Exception thrown when a rule fails validation.
## Usage

### gradle
Add the following to the `dependencies` of your project' `build.gradle`.
```groovy
dependencies {
    implementation project(':libs:rules-engine')
}
```
This will allow the `rules-engine` to be compiled if necessary and linked with your project.  You can set breakpoints in the library the same as you would your main project for debugging.

### ktor
You can now use this in your notifications-workflow-ktor service as follows.

```kotlin
/* The main class which subscribes the workflow execution
* for upload deadline check
* 
*/
class DeadLineCheckSubscriptionService(private val uri: String,
                                         private val authKey: String,
                                         private val containerName:String):KoinComponent {
    private val logger = KotlinLogging.logger {}
    private val cacheService: InMemoryCacheService = InMemoryCacheService()
    private val workflowEngine: WorkflowEngine = WorkflowEngine()
    private val notificationActivitiesImpl:NotificationActivitiesImpl = NotificationActivitiesImpl()

    private val repository: CosmosRuleRepository by inject { parametersOf(uri, authKey,"/ruleId", containerName) }
    private val workflowRulesEngine:WorkflowRulesEngine by inject { parametersOf(repository,logger) }
    /**
     *  The main method which executes workflow for uploadDeadline check
     *  @param subscription DeadlineCheckSubscription
     *  @return WorkflowSubscriptionResult
     */
    fun run(subscription: DeadlineCheckSubscription):
            WorkflowSubscriptionResult {
        try {
            val dataStreamId = subscription.dataStreamId
            val dataStreamRoute = subscription.dataStreamRoute
            val jurisdiction = subscription.jurisdiction
            val daysToRun = subscription.daysToRun
            val timeToRun = subscription.timeToRun
            val deliveryReference= subscription.deliveryReference
            val taskQueue = "notificationTaskQueue"

            val workflow =  workflowEngine.setupWorkflow(taskQueue,daysToRun,timeToRun,
                NotificationWorkflowImpl::class.java ,notificationActivitiesImpl, NotificationWorkflow::class.java)
            val execution =    WorkflowClient.start(workflow::checkUploadAndNotify, jurisdiction, dataStreamId, dataStreamRoute, daysToRun, timeToRun, deliveryReference)
            
            var workflowRule = getWorkflowRule(execution.workflowId, jurisdiction, daysToRun)
            workflowRule = workflowRulesEngine.addRule(workflowRule)
            return WorkflowSubscriptionResult(subscriptionId= workflowRule.ruleId, message="Successfully subscribed to the rule", deliveryReference=deliveryReference)
        }
        catch (e:Exception){
            logger.error("Error occurred while subscribing workflow for upload deadline: ${e.message}")
        }
        throw Exception("Error occurred while executing workflow engine to subscribe for upload deadline")
    }
    
     fun getWorkflowRule(ruleId:String,jurisdiction:String, daysToRun:List<String>):WorkflowRule{
        val id =  UUID.randomUUID().toString()
         val ruleName= "As a data provider, I would like to get a notification if an upload for my jurisdiction has not occurred by 12pm"
       val ruleConditions = mapOf(
            "jurisdiction" to "New York",
            "daysToRun" to  daysToRun
        )
        val emailAction = EmailNotificationAction(
            email= "xph6@cdc.gov",
            message ="Upload did not occur for $jurisdiction"
        )
        val workflowRule= WorkflowRule(id, ruleId, ruleName, ruleConditions,"ACTIVE", listOf<RuleAction>(emailAction))
       return workflowRule
    }
}
```