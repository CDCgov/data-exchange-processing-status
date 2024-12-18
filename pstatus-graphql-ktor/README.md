# Overview
The `GraphQL` service is designed to offer users a detailed view of their data across various stages, from upload and routing to validation, through `GraphQL Queries`. Users can also leverage `GraphQL Mutations` to create new reports, validate them, persist them to the database, or update existing reports. Additionally, the service provides the ability to `subscribe` to or `unsubscribe` from different types of notifications.
## Supported `Mutation` Endpoint Documentation:
| Name                                            | Return                         | Description                                                                                                                                                               |
|-------------------------------------------------|--------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| subscribeEmail(..)                              | SubscriptionResult             | Subscribe Email Notifications.                                                                                                                                            |
| unsubscribeEmail(..)                            | SubscriptionResult             | Unsubscribe Email Notifications.                                                                                                                                          |
| subscribeWebhook(..)                            | SubscriptionResult             | Subscribe Deadline Check lets you get notifications when an upload from jurisdictions has not happened by 12pm.                                                           |
| unsubscribeWebhook(..)                          | SubscriptionResult             | UnSubscribe Deadline Check lets you unsubscribe from getting notifications when an upload from jurisdictions has not happened by 12pm.                                    |
| subscribeDataStreamTopErrorsNotification(..)    | NotificationSubscriptionResult | Subscribe data stream top errors lets you subscribe to get notifications for top data stream errors and its frequency during an upload.                                   |
| unsubscribesDataStreamTopErrorsNotification(..) | NotificationSubscriptionResult | UnSubscribe data stream top errors lets you unsubscribe from getting notifications for top data stream errors and its frequency during an upload.                         |
| subscribeDeadlineCheck(..)                      | NotificationSubscriptionResult | Subscribe Deadline Check lets you get notifications when an upload from jurisdictions has not happened by 12pm.                                                           |
| unsubscribeDeadlineCheck(..)                    | NotificationSubscriptionResult | UnSubscribe Deadline Check lets you unsubscribe from getting notifications when an upload from jurisdictions has not happened by 12pm.                                    |
| subscribeUploadErrorsNotification(..)           | NotificationSubscriptionResult | Subscribe upload errors lets you get notifications when there are errors in an upload.                                                                                    |
| unsubscribeUploadErrorsNotification(..)         | NotificationSubscriptionResult | UnSubscribe upload errors lets you unsubscribe from getting notifications when there are errors during an upload.                                                         |
| subscribeUploadDigestCounts(..)                 | NotificationSubscriptionResult | Subscribe daily digest counts lets you get notifications with the counts of all jurisdictions for a given set of data streams after the prescribed time to run is past.   |
| unsubscribeUploadDigestCounts(..)               | NotificationSubscriptionResult | UnSubscribe daily digest counts lets you get notifications with the counts of all jurisdictions for a given set of data streams after the prescribed time to run is past. |
| upsertReport(..)                                | Report                         | Creates a new report or replace an existing one based on specified action.                                                                                                |

## Supported `Query` Endpoint Documentation:
| Name                                      | Return                              | Description                                                                                                                 |
|-------------------------------------------|-------------------------------------|-----------------------------------------------------------------------------------------------------------------------------|
| getHealth(..)                             | GraphQLHealthCheck                  | Health status.                                                                                                              |
| getReports(..)                            | list of reports                     | Reports associated with the provided upload ID..                                                                            |
| getSubmissionDetails(..)                  | SubmissionDetails                   | Submission details for the provided upload ID.                                                                              |
| searchReports(..)                         | list of reports                     | List of reports based on ReportSearchParameters options.                                                                    |
| hl7DirectIndirectMessageCounts(..)        | HL7DirectIndirectMessageCounts      | HL7v2 message counts using both a direct and an indirect counting method for the provided parameters.                       |
| hl7InvalidMessageCounts(..)               | HL7InvalidMessageCounts             | HL7v2 invalid message counts using both a direct and an indirect counting method for the provided parameters.               |
| hl7InvalidStructureValidationCounts(..)   | hl7InvalidStructureValidationCounts | HL7v2 invalid structure counts for the provided parameters.                                                                 |
| processingCounts(..)                      | ProcessingCounts                    | Processing counts for the provided parameters.                                                                              |
| reportCountsWithParams(..)                | AggregateReportCounts               | Detailed counts within each stage for each matching upload with the provided parameters.                                    |
| reportCountsWithUploadId(..)              | ReportCounts                        | Detailed counts within each stage for the provided uploadId.                                                                |
| rollupCountsByStage(..)                   | list of stage counts                | Rolled up counts by stage for the provided parameters.                                                                      |
| getDeadLetterReportsByDataStream(..)      | list of dead letter reports         | All the dead-letter reports associated with the provided datastreamId, datastreamroute and timestamp date range.            |
| getDeadLetterReportsByUploadId(..)        | list of dead letter reports         | All the dead-letter reports associated with the provided uploadId.                                                          |
| getDeadLetterReportsCountByDataStream(..) | int, number of reports              | Count of dead-letter reports associated with the provided datastreamId, (optional) datastreamroute and timestamp date range |
| searchDeadLetterReports(..)               | list of dead letter reports         | Dead-letter reports based on ReportSearchParameters options.                                                                |
| getUploadStats(..)                        | UploadStats                         | Various uploads statistics.                                                                                                 |
| getUploads(..)                            | UploadsStatus                       | Upload statuses for the given filter, sort, and pagination criteria.                                                        |




## Environment Variable Setup

### Database
The `DATABASE` environment variable is used to specify the database used for persisting reports. Supported databases are:
 - `cosmos`
 - `dynamo`
 - `couchbase`
 - `mongo`

For Cosmos DB only, set the following environment variables:

 - `COSMOS_DB_CLIENT_ENDPOINT` - your Cosmos DB client endpoint.
 - `COSMOS_DB_CLIENT_KEY` - Your Cosmos DB client key.

For Dynamo DB only, set the following environment variables:
 - `DYNAMO_TABLE_PREFIX` - Table prefix to be used for the reports and deadletter reports.
 - `AWS_ACCESS_KEY_ID` - The Access Key ID for an IAM user with permissions to read/write to and from the database.
 - `AWS_SECRET_ACCESS_KEY` - The secret access key for an IAM user with permissions to read/write to and from the database.

For Couchbase DB only, set the following environment variables:
 - `COUCHBASE_CONNECTION_STRING` - URI of the couchbase database.
 - `COUCHBASE_USERNAME` - Username for the couchbase database.
 - `COUCHBASE_PASSWORD` - Password for the username provided.

For Mongo DB only, set the following environment variables:
 -`MONGO_CONNECTION_STRING` - URI of the couchbase database.
 - `MONGO_DATABASE_NAME` - Name of the database. For example, "ProcessingStatus".
 - 
### GRAPHQL 
- `GRAPHQL_PATH` - The path of the `GraphQL endpoint`.

### Security 
- `SECURITY_ENABLED` - Set to false for local development. 

### Notifications
- `PSTATUS_NOTIFICATIONS_BASE_URL` - The notifications service base url.

### Workflows
- `PSTATUS_WORKFLOW_NOTIFICATIONS_BASE_URL` - The workflows service base url.

# Publishing
There are several ways to publish the 
## Publish to CDC's ImageHub
With one gradle command you can build and publish the project's Docker container image to the external container registry, imagehub, which is a nexus repository.

To do this, we use Google [jib](https://cloud.google.com/java/getting-started/jib), which vastly simplies the build process as you don't need a docker daemon running in order to build the Docker container image.

Inside of build.gradle `jib` section are the authentication settings.  These use environment variables for the username and password, which are `IMAGEHUB_USERNAME` and `IMAGEHUB_PASSWORD` respectively.
```commandline
$ gradle jib
```
The location of the deployment will be to the `docker-dev2` repository under the folder `/v2/dex/pstatus`. 

## Publish to CDC's Quay.io Repo
The purpose of publishing to quay.io is to make the container images publicly available so they can easily be pulled
down and run in podman or docker without the need to build anything.

```shell
$ gradle jib \
    -Djib.to.image=quay.io/us-cdcgov/phdo/pstatus-graphql:latest \
    -Djib.to.auth.username='us-cdcgov+github_ci_phdo' \
    -Djib.to.auth.password=$PASSWORD
```
Replace the`PASSWORD` value with the one for the `us-cdcgov+github_ci_phdo` robot account. 

## Publish to CDC's Azure Container Registry
This is a less common approach and is likely only considered if the code will be run within an Azure container instance
or app service.

```shell
$ gradle publishImageToLocalRegistry
$ az login
$ az acr login --name ociodexdevprocessingstatus
$ docker login ociodexdevprocessingstatus.azurecr.io
$ docker tag pstatus-graphql-ktor ociodexdevprocessingstatus.azurecr.io/pstatus-graphql-ktor:v1
$ docker push ociodexdevprocessingstatus.azurecr.io/pstatus-graphql-ktor:v1 
```
For the docker login step, provide the username and password found in the ACR > Access Keys.  The username will be `ociodexdevprocessingstatus.azurecr.io`.

### Health Check
The api endpoint **"getHealth"** can be used to check the health of the service. It will also internally verify if the Cosmos DB is UP.

**{{ps_api_base_url}}/graphql/getHealth**

