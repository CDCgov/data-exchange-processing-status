# notifications-workflow-ktor
The `notifications-workflow-kotr`microservice is a workflow orchestration service for processing and evaluating the active notification rules.  The service uses temporal as its workflow engine for scheduling , orchestrating and then executing the activity stub related to the notification rules

# Structure Overview  

## workflow:
This folder contains all the temporal workflow interfaces and its corresponding implementation for all the active notification rules which has the method signatures on what to do for each of the active notifications and its implementation classes as well.
The workflow interfaces defined at this time are as follows : -

### DataStreamTopErrorsNotificationWorkflow
The interface which defines the digest counts and top errors during an upload and its frequency
### NotificationWorkflow
The interface which define the upload error and notify method
### UploadErrorsNotificationWorkflow
Interface that defines the upload errors and notify

The corresponding implementation classes are as follows: -

### DataStreamTopErrorsNotificationWorkflowImpl
The implementation class which determines the digest counts and top errors during an upload and its frequency
### NotificationWorkflowImpl
The implementation class for notifying if an upload has not occurred within a specified time
### UploadErrorsNotificationWorkflowImpl
The implementation class for errors on missing fields from a upload

The implementation class not only defines the workflow interface methods but also does the activity processing when the orchestrator fires the workflow rules. In order for the activity to trigger each of the implementation class instantiates the activity stub using the corresponding activity interface and defines the start to close, schedule to close and the retry options

## temporal: 

### WorkflowEngine
This workflow engine class which sets up the temporal workflow service stubs and the factory instance which in turn generates the new worker which is used for registering the workflow and the activity implementations. This is a generic class used by all services which calls the setup method which sets up the temporal workflow engine.
Workflow engine class which creates a grpC client instance of the temporal server using which it registers the workflow and the activity implementation
Also,using the workflow options the client creates a new workflow stub.

## service:

### DataStreamTopErrorsNotificationSubscriptionService
The main class which sets up and subscribes the workflow execution for digest counts and the frequency with which each of the top 5 errors occur
### DataStreamTopErrorsNotificationUnSubscriptionService
The main class which subscribes the workflow execution for digest counts and top errors and its frequency for each upload
### DeadLineCheckSubscriptionService
The main class which subscribes the workflow execution for upload deadline check
### DeadLineCheckUnSubscriptionService
The main class which unsubscribes the workflow execution for upload errors
### UploadErrorsNotificationSubscriptionService
The main class which subscribes the workflow execution for upload errors
### UploadErrorsNotificationUnSubscriptionService
The main class which unsubscribes the workflow execution for upload errors

## model: 

### ErrorDetail
Error Detail class
### NotificationSubscriptionResponse 
Notification subscription response class
### Subscription
DeadlineCheckSubscription data class which is serialized back and forth when we need to unsubscribe the workflow by the subscriptionId

## email: 

### EmailDispatcher
The class which dispatches the email using SMTP

## activity:

## NotificationActivity
Interface which defines the activity methods

### NotificationActivityImpl
Implementation class for sending email notifications for various notifications```

## Routes 

The class which defines the routes that will be the endpoints for the graphQL client to invoke and thereby subscribe/unsubscribe to the workflow active notification rules.

###  Route.subscribeDeadlineCheckRoute
Route to subscribe for DeadlineCheck subscription

### Route.unsubscribeDeadlineCheck
Route to unsubscribe for DeadlineCheck subscription

### Route.subscribeUploadErrorsNotification
Route to subscribe for upload errors notification subscription

###  Route.unsubscribeUploadErrorsNotification
Route to unsubscribe for upload errors subscription notification

### Route.subscribeDataStreamTopErrorsNotification
Route to subscribe for top data stream errors notification subscription

### Route.unsubscribesDataStreamTopErrorsNotification
Route to unsubscribe for top data stream errors notification subscription

### Route.healthCheckRoute
Route for Temporal Server health check