# Overview
This project is the processing status report sink.  It listens for messages on an Azure Service bus queues and topics, validates the messages, and if validated persists them to CosmosDB. If the validation fails due to missing fields or malformed data, then the message is persisted in cosmosdb under a new dead-letter container and the message is also sent to the dead-letter queue under the configured topic subscription(if the message was processed using the topic listener) 

This is a microservice built using Ktor that can be built as a docker container image.

## Publish to CDC's imagehub
With one gradle command you can builds and publish the project's Docker container image to the external container registry, imagehub, which is a nexus repository.

To do this, we use Google [jib](https://cloud.google.com/java/getting-started/jib), which vastly simplies the build process as you don't need a docker daemon running in order to build the Docker container image.

Inside of build.gradle `jib` section are the authentication settings.  These use environment variables for the username and password, which are `IMAGEHUB_USERNAME` and `IMAGEHUB_PASSWORD` respectively.
```commandline
gradle jib
```
The location of the deployment will be to the `docker-dev2` repository under the folder `/v2/dex/pstatus`. 