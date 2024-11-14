# Overview
TO DO- combine below Reports, DeadLetterReports, Notifications section into an overview

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

## Graphql Service supported `Mutation` Endpoint Documentation:
| Name         | Description                                    |
|--------------|------------------------------------------------|
| upsertReport | Create a new report or replace an existing one |

## Graphql Service supported `Query` Endpoint Documentation

| Name      | Description  |
|-----------|--------------|
| getHealth | health check |



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

