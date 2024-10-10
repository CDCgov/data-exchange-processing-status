# Overview
This project is the processing status event-reader-sink service. Based on the configurations, it does the following:
- Listens for messages on an Azure Service bus and sinks them into the Blob Storage, or,
- Listens for messages on the AWS SNS Topic and sinks them into S3.

This is a microservice built using Ktor that can be built as a docker container image.

## Publish to CDC's Azure Container Registry
```commandline
$ gradle publishImageToLocalRegistry
$ az login
$ az acr login --name ociodexdevprocessingstatus
$ docker login ociodexdevprocessingstatus.azurecr.io
$ docker tag pstatus-graphql-ktor ociodexdevprocessingstatus.azurecr.io/pstatus-graphql-ktor:v1
$ docker push ociodexdevprocessingstatus.azurecr.io/event-reader-sink:v1 
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