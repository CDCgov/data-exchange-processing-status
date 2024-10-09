# Overview
This project is the processing status service. The pstatus-graph-ktor service provides the following features:

Ability to query for existing reports, uploads, upload status.
Validating the messages, and if validated persists them to CosmosDB. 
Provides the feature for a user to subscribe or unsubscribe to email notifications.
Workflows.

This is a microservice built using Ktor that can be built as a docker container image.

## Environment Variable Setup

### Database

For Cosmos DB only, set the following environment variables:

COSMOS_DB_CLIENT_ENDPOINT - your Cosmos DB client endpoint.
COSMOS_DB_CLIENT_KEY - Your Cosmos DB client key.

### GRAPHQL 
GRAPHQL_PATH - The path for the GraphQL endpoint.

### Security 
SECURITY_ENABLED - Boolean value. Set to false for development purposes in the local development environments.

### Notifications
PSTATUS_NOTIFICATIONS_BASE_URL - The notifications service base url.

### Workflows
PSTATUS_WORKFLOW_NOTIFICATIONS_BASE_URL - The workflows service base url.


## Publish to CDC's Azure Container Registry
```commandline
$ gradle publishImageToLocalRegistry
$ az login
$ az acr login --name ociodexdevprocessingstatus
$ docker login ociodexdevprocessingstatus.azurecr.io
$ docker tag pstatus-graphql-ktor ociodexdevprocessingstatus.azurecr.io/pstatus-graphql-ktor:v1
$ docker push ociodexdevprocessingstatus.azurecr.io/pstatus-graphql-ktor:v1 
```
For the docker login step, provide the username and password found in the ACR > Access Keys.  The username will be `ociodexdevprocessingstatus.azurecr.io`.

## Publish to CDC's ImageHub
With one gradle command you can builds and publish the project's Docker container image to the external container registry, imagehub, which is a nexus repository.

To do this, we use Google [jib](https://cloud.google.com/java/getting-started/jib), which vastly simplies the build process as you don't need a docker daemon running in order to build the Docker container image.

Inside of build.gradle `jib` section are the authentication settings.  These use environment variables for the username and password, which are `IMAGEHUB_USERNAME` and `IMAGEHUB_PASSWORD` respectively.
```commandline
$ gradle jib
```
The location of the deployment will be to the `docker-dev2` repository under the folder `/v2/dex/pstatus`. 