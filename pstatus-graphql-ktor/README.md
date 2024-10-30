# Overview
This project is part of the processing status suite of services, named, "pstatus-graphql-ktor". This is a microservice built using Ktor that can be built as a docker container image. The pstatus-graphql-ktor service provides the following features:

### Reports:
Ability to query for existing reports, uploads, upload status.
Validating the messages, and, if validated, persists them to CosmosDB. 

### DeadLetterReports:
- Ability to look for dead letter reports based on the search criteria.
- Ability to retrieve dead letter reports stats based on the search criteria.

### Notifications:
Provides the feature for a user to subscribe or unsubscribe to different types of Notifications as follows:

- Top data stream errors and its frequency during an upload.
- Upload errors when there are errors in an upload.
- Email Notifications.
- Webhook Notifications.
- Deadline Checks.

## Environment Variable Setup

### Database

For Cosmos DB only, set the following environment variables:

- COSMOS_DB_CLIENT_ENDPOINT - your Cosmos DB client endpoint.
- COSMOS_DB_CLIENT_KEY - Your Cosmos DB client key.

### GRAPHQL 
- GRAPHQL_PATH - The path for the GraphQL endpoint.

### Security 
- SECURITY_ENABLED - Boolean value. Set to false for development purposes in the local development environments.

### Notifications
- PSTATUS_NOTIFICATIONS_BASE_URL - The notifications service base url.

### Workflows
- PSTATUS_WORKFLOW_NOTIFICATIONS_BASE_URL - The workflows service base url.

## List of all the Endpoints available for Querying:

### getHealth
Provides the Status of the service dependencies.

### getReports
Returns all the reports associated with the provided upload ID.

### getSubmissionDetails
Returns the submission details for the provided upload ID.

### getDeadLetterReportsByDataStream
Return all the dead-letter reports associated with the provided datastreamId, datastreamroute and timestamp date range.

### getDeadLetterReportsByUploadId
Return all the dead-letter reports associated with the provided uploadId.

### getDeadLetterReportsCountByDataStream
Return count of dead-letter reports associated with the provided datastreamId, (optional) datastreamroute and timestamp date range.

### getUploadStats
Return various uploads statistics.

### getUploads
Get the upload statuses for the given filter, sort, and pagination criteria.


## List of all the Endpoints available for Mutations

### subscribeEmail
Subscribe Email Notifications.

### unsubscribeEmail
Unsubscribe Email Notifications.

### subscribeWebhook
Subscribe Webhook Notifications.

### unsubscribeWebhook
Unsubscribe Webhook Notifications.

### subscribeDataStreamTopErrorsNotification
Subscribe data stream top errors lets you subscribe to get notifications for top data stream errors and its frequency during an upload.

### unsubscribesDataStreamTopErrorsNotification
UnSubscribe data stream top errors lets you unsubscribe from getting notifications for top data stream errors and its frequency during an upload.

### subscribeDeadlineCheck
Subscribe Deadline Check lets you get notifications when an upload from jurisdictions has not happened by 12pm.

### unsubscribeDeadlineCheck
UnSubscribe Deadline Check lets you unsubscribe from getting notifications when an upload from jurisdictions has not happened by 12pm.

### subscribeUploadErrorsNotification
Subscribe upload errors lets you get notifications when there are errors in an upload.

### unsubscribeUploadErrorsNotification
UnSubscribe upload errors lets you unsubscribe from getting notifications when there are errors during an upload.

### upsertReport
Create a new or replace an existing report. 

# Publishing
There are several ways to publish the 
## Publish to CDC's ImageHub
With one gradle command you can builds and publish the project's Docker container image to the external container registry, imagehub, which is a nexus repository.

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
    -Djib.to.image=quay.io/us-cdcgov/phdo/pstatus-report-sink:latest \
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

