# subscription-management
The `subscriptions-management` library is used for managing notification subscriptions.  Notification subscriptions are setup by end-users to get automatically notified when conditions are met through a variety of delivery mechanisms.  This library will provide the means to define and manage those subscriptions.

# Structure Overview  

## Interfaces:

### SubscriptionManagementEngine: 
Core Engine interface for managing subscriptions (CRUD operations).
### SubscriptionManagementRepository: 
Interface which defines the CRUD operations to perform on CosmosDB using the CosmosRepository from commons-database library
### WorkflowSubscription: 
Interface which defines the work flow subscription attributes.

## Models

### WorkflowActivity
The class which defines the WorkflowActivity attributes

## Repository

## CosmosSubscriptionManagementRepository
The class which implements the interface SubscriotionsManagementRepository and uses the Cosmos Repository from commons-database lib
to persist subscriptions, conditions, and actions.

## Implementation class

### SubscriptionManagementEngineImpl: 
The default implementation class which gets exposed to the other apps and libs and which abstracts the
cosmos db CRUD operations.This uses the persistence layer the CosmosRuleRepository to perform the CRUD

### SubscriptionValidationUtils: 
Utility class for validating subscriptions before they are processed by the Engine.

## Exception

### SubscriptionNotFoundException
The exception thrown when a subscription is not found in Cosmos Db
### SubscriptionManagementException
The default exception which gets thrown during the CRUD operations

### SubscriptionValidationException
Exception thrown when a subscription fails validation.
## Usage

### gradle
Add the following to the `dependencies` of your project' `build.gradle`.
```groovy
dependencies {
    implementation project(':libs:subscription-management')
}
```
This will allow the `subscription-management` to be compiled if necessary and linked with your project.  You can set breakpoints in the library the same as you would your main project for debugging.

### ktor
You can now use this in your notifications-workflow-ktor service as follows.

```kotlin
fun KoinApplication.loadKoinModules(environment: ApplicationEnvironment): KoinApplication {
    val cosmosModule = module {
        val uri = environment.config.property("azure.cosmos_db.client.endpoint").getString()
        val authKey = environment.config.property("azure.cosmos_db.client.key").getString()
        single<ProcessingStatusRepository> {
            CosmosRepository(
                uri,
                authKey,
              "/notificationId",
              "NotificationSubscriptions",
            )
        } 
        single(createdAtStart = true) { CosmosConfiguration(uri, authKey) }
    }
    return modules(listOf(cosmosModule))
}

/* The main class which subscribes the workflow execution
* for upload deadline check
* 
*/
class DeadLineCheckSubscriptionService: KoinComponent {
    private val logger = KotlinLogging.logger {}
    private val workflowEngine: WorkflowEngine = WorkflowEngine()
    private val notificationActivitiesImpl:NotificationActivitiesImpl = NotificationActivitiesImpl()

    private val cosmosRepository by inject<ProcessingStatusRepository>()
    private val repository by inject<CosmosSubscriptionManagementRepository>(){ parametersOf(cosmosRepository,logger) }
    private val subscriptionManagementEngine:SubscriptionManagementEngine by inject { parametersOf(repository) }
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
            val cronSchedule = subscription.cronSchedule
            val emailAddresses = subscription.emailAddresses
            val taskQueue = "notificationTaskQueue"

            val workflow = workflowEngine.setupWorkflow(taskQueue, cronSchedule,
                NotificationWorkflowImpl::class.java ,notificationActivitiesImpl, NotificationWorkflow::class.java)
            val execution = WorkflowClient.start(workflow::checkUploadAndNotify, jurisdiction, dataStreamId, dataStreamRoute, cronSchedule, emailAddresses)
            
            val workflowSubscription= getWorkflowSubscription(execution.workflowId, jurisdiction, cronSchedule)
            val subscriptionId = subscriptionManagementEngine.subscribe(workflowSubscription)
            return WorkflowSubscriptionResult(subscriptionId= subscriptionId, message="Successfully subscribed to the rule", emailAddresses=emailAddresses)
        }
        catch (e:Exception){
            logger.error("Error occurred while subscribing workflow for upload deadline: ${e.message}")
        }
        throw Exception("Error occurred while executing workflow engine to subscribe for upload deadline")
    }
    
     fun getWorkflowSubscription(notificationId:String,jurisdiction:String, cronSchedule: String): WorkflowSubscription{
       val notification= "As a data provider, I would like to get a notification if an upload for my jurisdiction has not occurred by 12pm"
       val conditions = mapOf(
            "jurisdiction" to "New York",
            "cronSchedule" to cronSchedule
        )
        val emailAction = EmailNotificationAction(
            email= "xph6@cdc.gov",
            message ="Upload did not occur for $jurisdiction"
        )
        val workflowRule= WorkflowSubscription(null, notificationId, notification, conditions,"ACTIVE", listOf<WorkflowActivity>(emailAction))
       return workflowRule
    }
}
```