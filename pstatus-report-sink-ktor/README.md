# Overview
This project is the processing status report sink.  It listens for messages on an Azure Service bus, validates the messages, and if validated persists them to CosmosDB.

This is a microservice built using Ktor that can be built as a docker container image.

## Build docker container
```commandline
gradle buildImage
```

## Publish to imagehub
Builds and publishes the project's Docker image to the external registry, imagehub.
```commandline
gradle publishImage
```
The settings for publishing the image are defined in the build.gradle.kts ktor.docker section.