# Overview
This project is the event-reader-sink service, uses Camel Routing. Based on the Camel Route Configurations, it does the following:
- Listens for messages on an Azure Service bus and sinks them into the Blob Storage, or,
- Listens for messages on the AWS SNS Topic and sinks them into S3.

This is a microservice built using Ktor that can be built as a docker container image.

### Supported Messaging Systems
- AWS (SQS Queue/ SNS Topic)
- Azure (Service Bus Queue/ Topic)

### Environment Variable Setup
Set the following environment variables
#### Configuration to specify which cloud provider to use
The CLOUD_PROVIDER can either be `aws` or `azure`.
- `CLOUD_PROVIDER` - The cloud provider we will be using (Set to either 'aws' or 'azure' based on which service provider we want to connect to).

#### AWS
For AWS only, set the following environment variables:
- `AWS_ACCESS_KEY_ID` - The Access Key ID for an IAM user with permissions to receive and delete messages from specified SQS queue.
- `AWS_SECRET_ACCESS_KEY` - The secret access key for an IAM user with permissions to receive and delete messages from the specified SQS queue. This key is used for authentication and secure access to the queue.
- `AWS_SQS_REGION` - The AWS region where your SQS queue is located.
- `AWS_QUEUE_URL` - URL of the Amazon Simple Queue Service(SQS) queue.
- `AWS_S3_BUCKET_NAME` - The AWS S3 bucket name.
- `AWS_S3_REGION` - The AWS region where your S3 bucket is located.

#### Azure
For Azure only, set the following environment variables:
- `SERVICE_BUS_CONNECTION_STRING` - Your service bus connection string.
- `SERVICE_BUS_NAMESPACE` - The namespace of the service bus.
- `SERVICE_BUS_SUBSCRIPTION_NAME` - The subscription name of the service bus.
- `SERVICE_BUS_SHARED_ACCESS_KEY_NAME` - The service bus shared access key name.
- `SERVICE_BUS_SHARED_ACCESS_KEY` - The service bus shared access key value.
- `SERVICE_BUS_TOPIC_NAME` - The service bus topic name.
- `AZURE_BLOB_STORAGE_ACCOUNT_NAME` - The Azure blob storage account name.
- `AZURE_BLOB_STORAGE_ACCOUNT_KEY` - The Azure blob storage account key.
- `AZURE_BLOB_ENDPOINT_URL` - The storage account endpoint url.
- `AZURE_BLOB_CONTAINER_NAME`- The Azure blob storage container name.



## Publish to CDC's Azure Container Registry
```commandline
$ gradle publishImageToLocalRegistry
$ az login
$ az acr login --name ociodexdevprocessingstatus
$ docker login ociodexdevprocessingstatus.azurecr.io
$ docker tag event-reader-sink ociodexdevprocessingstatus.azurecr.io/event-reader-sink:v1
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
