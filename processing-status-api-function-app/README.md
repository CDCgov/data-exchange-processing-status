# Processing Status API Function App

## Setup

The following is needed in order to build and deploy this function app:

- [Azure CLI](https://learn.microsoft.com/en-us/cli/azure/install-azure-cli)
- [Azure Functions Core Tools](https://learn.microsoft.com/en-us/azure/azure-functions/functions-run-local?tabs=v4%2Clinux%2Ccsharp%2Cportal%2Cbash#v2)
- [gradle](https://gradle.org/install/)

## Required Application Settings
In addition to the standard required application settings, the processing status API function app requires the following.

- `CosmosDbEndpoint` - URL of the cosmos database
- `CosmosDbKey` - shared key used to connect to the cosmos database
- `ServiceBusQueueName` - service bus queue name, which should always be `processing-status-cosmos-db-queue`
- `ServiceBusConnectionString` - connection string for the service bus

## Build and Deploy

To build and deploy you can use the Azure Functions Gradle plugin. You can do this with the following command:

 ```
 gradle azureFunctionsDeploy -Dsubscription=<subcription_id> -DresourceGroup=<resource_group> -DappName=<function_app_name>
 ```
 Replace the `subscription`, `resourceGroup` and `appName` parameters with the actual values.
 
## Code coverage metrics

JaCoCo plugin provides code coverage metrics. The JacocoReport task has been added to build.gradle to generate code coverage reports
in different formats. 

To check the code coverage, run the following command:

```
./gradlew test jacocoTestReport 
```

The html version of the code coverage report can be found in build/jacocoHtml folder, index.html.