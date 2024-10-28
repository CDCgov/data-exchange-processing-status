# Overview
Reports are an essential component of the data observability aspect of the CDC Data Exchange (DEX).  In DEX, data is ingested to the system typically through a file upload.  As the upload progresses through the service line processing occurs.  The processing in the service line is made up stages, which can be the upload, routing, data validation, data transformations, etc.  Within each of those stages one or more actions may occur.  Taking the example of upload, one action within the stage may be to first verify that all the required metadata associated with the uploaded file is provided and reject it if not.  Other upload actions may include the file upload itself or the disposition of the upload for further downstream processing.  Reports are provided by both services internal to DEX and downstream of DEX as data moves through CDC systems.  Those services indicate the processing status of these stages through Reports.

## Report Sinking
This project is the processing status report sink.  It listens for messages on an Azure Service bus queues and topics or RabbitMQ queues(for local runs) validates the messages, and if validated persists them to CosmosDB. If the validation fails due to missing fields or malformed data, then the message is persisted in cosmosdb under a new dead-letter container and the message is also sent to the dead-letter queue under the configured topic subscription(if the message was processed using the topic listener) 

This microservice is built using Ktor and can be built as a docker container image.

### Environment Variable Setup
#### Database
The `DATABASE` environment variables is used to specify the database used for persisting processing status data.
Set this variable to one of the following values:
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
- `MONGO_CONNECTION_STRING` - URI of the couchbase database.
- `MONGO_DATABASE_NAME` - Name of the database. For example, "ProcessingStatus".

#### Message System
- The `MSG_SYSTEM` environment variable is used to determine which system will be loaded dynamically.
Set this variable to one of the following values:
- `AZURE_SERVICE_BUS`
- `RABBITMQ`
- `AWS`

For Azure Service Bus only, set the following environment variables:
- `SERVICE_BUS_CONNECTION_STRING` - Your service bus connection string.
- `SERVICE_BUS_REPORT_QUEUE_NAME/SERVICE_BUS_REPORT_TOPIC_NAME` - Your service bus queue or topic name.
- `SERVICE_BUS_REPORT_TOPIC_SUBSCRIPTION_NAME` - Your service bus topic subscription.

For RabbitMQ(Local Runs) only,  set the following environment variables:
- `RABBITMQ_HOST` - if not provided, `localhost` will be used.
- `RABBITMQ_PORT` - If not provided, default port `5672` will be used.
- `RABBITMQ_USERNAME` - if not provided, default `guest` will be used.
- `RABBITMQ_PASSWORD` - if not provided, default `guest` will be used.
- `RABBITMQ_REPORT_QUEUE_NAME` - Your RabbitMQ queue name bound to the desired exchange topic.
- `RABBITMQ_VIRTUAL_HOST` - if not provided, default virtual host `/` will be used.

For AWS SNS/SQS only, set the following environment variables:
- `AWS_SQS_URL` - URL of the Amazon Simple Queue Service(SQS) queue that the Ktor module will interact with to receive, process and delete messages.
- `AWS_ACCESS_KEY_ID` - The Access Key ID for an IAM user with permissions to receive and delete messages from specified SQS queue.
- `AWS_SECRET_ACCESS_KEY` - The secret access key for an IAM user with permissions to receive and delete messages from the specified SQS queue. This key is used for authentication and secure access to the queue.
- `AWS_REGION` (Optional) - The AWS region where your SQS queue is located, if not provided, default region `us-east-1` will be used

# Publish to CDC's ImageHub
With one gradle command you can build and publish the project's Docker container image to the external container registry, imagehub, which is a nexus repository.

To do this, we use Google [jib](https://cloud.google.com/java/getting-started/jib), which vastly simplifies the build process as you don't need a docker daemon running in order to build the Docker container image.

Inside of build.gradle `jib` section are the authentication settings.  These use environment variables for the username and password, which are `IMAGEHUB_USERNAME` and `IMAGEHUB_PASSWORD` respectively.
```commandline
gradle jib
```
The location of the deployment will be to the `docker-dev2` repository under the folder `/v2/dex/pstatus`.

# Report Delivery Mechanisms
Reports may be provided in one of four ways - either through calls into the Processing Status (PS) API as GraphQL mutations, by way of an Azure Service Bus, AWS SNS/SQS or using RabbitMQ.  There are pros and cons of each summarized below.

| Azure Service Bus   | AWS SQS            | GraphQL                  | RabbitMQ(Local Runs)                        |
|---------------------|--------------------|--------------------------|---------------------------------------------|
| Fire and forget [1] | Fire and forget[1] | Confirmation of delivery | Fire and forget [1], publisher confirms [2] |
| Fast                | Fast               | Slower                   | Fast and lightweight                        |

[1] Failed reports are sent to a Report dead-letter that can be queried to find out the reason(s) for its rejection.  When using Azure Service Bus or AWS SNS/SQS there is no direct feedback mechanism to the report provider of the rejection.
[2] Publisher confirms mode can be enabled, which provides asynchronous way to confirm that message has been received.

### GraphQL Mutations
GraphQL mutations are writes to a persisted object.  In the case of PS API, reports are written to PS API as GraphQL mutations.

For context, GraphQL does not require any special client and can be communicated to the same as REST endpoints.  However, unlike REST there is only one endpoint with path `/graphql` and you POST to it.  The main difference is in the request body of the POST.  Below is an example of how a Report would be sent to PS API.

**POST** `{{ps_api_base_url}}/graphql`

Request body:
```graphql
mutation AddReport($report: Report!) {
  addReport(report: $report) {
    reportId
    result
    issues
  }
}
```
In this example, we are asking for the `reportId` of the added report be returned in the response.

Response of accepted report:
```json
{
  "data": {
    "addReport": {
      "reportId": "47286e48-2a22-4e26-930e-c7b4115b0cf1",
      "result": "SUCCESS",
      "issues": null
    }
  }
}
```
Response of rejected report:
```json
{
  "data": {
    "addReport": {
      "reportId": null,
      "result": "FAILURE",
      "issues": [
        "Missing required field, dex_ingest_datetime"
      ]
    }
  }
}
```
> NOTE: With GraphQL, every HTTP status code returned is a 200 unless the request is unauthorized or something fails on the server.  Clients must inspect the `result` field to determine success.

There will also be a mutation available to replace an existing report.

### Azure Service Bus
Reports may be sent to the PS API Azure Service Bus (ASB) queue or topic.  Below is an example code snippet in Kotlin.

```kotlin
val report = MyDEXReport().apply {
    // set the report fields
}

val senderClient = ServiceBusClientBuilder()
    .connectionString(sbConnString)
    .sender()
    .topicName(topicName)
    .buildClient()

// Send the report to the PS API report topic
senderClient.sendMessage(ServiceBusMessage(report))
```

> Sending reports via the PS API ASB report **queue** is being deprecated.  The PS API report queue will eventually be removed.  Reports should be sent to the PS API report **topic** instead.

In order to access the ASB from your DEX service there may need be a firewall rule put in place.  If your service is running in Kubernetes then no firewall rule should be necessary.

### RabbitMQ
The reports maybe sent to RabbitMQ queue running locally.

#### How to Start RabbitMQ Server  With Docker Compose
1. Ensure that you have `Docker` and `Docker Compose` installed on your local system.
2. Navigate to root directory of `pstatus-report-sink-ktor`
3. Run the following command to start RabbitMQ server in detached mode:
```bash
   docker-compose up -d 
   ```
#### How to send reports to RabbitMQ Server
There are two ways reports can be sent  through management UI and programmatically.
1. Navigate to `http://localhost:15672` in your browser to access RabbitMQ Management UI. This interface allows you create and manage exchanges, define topics, set up and bind queues to exchanges. Additionally, You can publish messages directly to queues through this UI.
2. Sending reports programmatically:

```kotlin
val report = MyDEXReport().apply {
    // set the report fields

}
val factory = connectionFactory()
    // configure factory further with RabbitMQ server

factory.newConnection().use { connection: com.rabbitmq.client.ConnectionFactory  ->
    connection.createChannel().use { channel: com.rabbitmq.client.Connection ->
        val queueName = "your queue name"
        //convert the report to ByteArray
        val message = report.toByteArray()
        //publish message to existing queue
        channel.basicPublish("", queueName, null, message)
        //RabbitMQ supports more sophisticated publishing mechanisms as well
    }
}
```
### AWS SNS/SQS
The reports may be sent to PS API AWS SQS queue.
#### How to send reports to AWS SNS/SQS
There are two ways reports can be sent  through AWS Console and programmatically.
1. Using AWS Console:
    - Navigate to AWS Management Console in your browser.
    - Access the SNS Topic Subscription page, where you can view and manage SNS topics and their associated subscriptions.
    - Select the desired SNS topic that is configured to send messages to your queue. You can directly `publish messages` to a topic.
2. Sending reports programmatically:

```kotlin
val report = MyDEXReport().apply {
    // set the report fields
}
suspend fun createSQSClient(): SqsClient{
    return SqsClient{}
}
suspend fun sendReportsToSQS(queueURL: String, report: String){
    val sqsClient = createSQSClient()
    try {
        val requestToSendReport = SendMessageRequest{
            message = report
           this.queueURL = queueURL
        }
       val response = sqsClient.sendMessage(requestToSendReport)
       logger.infor("The report sent, response received: $response")
    }catch (ex:SqsException){
        logger.error("Failed to send the message: $ex.message")
    }finally {
        sqsClient.close()
    }
}
```
# Checking on Reports
GraphQL queries are available to look for reports, whether they were accepted by PS API or not.  If a report can't be ingested, typically due to failed validations, then it will go to deadletter.  The deadletter'd reports can be searched for and the reason(s) for its failure examined.   

The following queries will provide all reports for the given upload ID, whether accepted or not (sent to deadletter):
```graphql
query GetReports($uploadId: String!) {
    getReports(uploadId: $uploadId,
        reportsSortedBy: "timestamp",
        sortOrder: Ascending) {
        id
        uploadId
        dataStreamId
        dataStreamRoute
        messageId
        reportId
        stageName
        status
        timestamp
        content
        contentType
    }
    getDeadLetterReportsByUploadId(uploadId: $uploadId) {
        id
        uploadId
        dataStreamId
        dataStreamRoute
        dispositionType
        messageId
        reportId
        stageName
        status
        timestamp
        validationSchemas
        deadLetterReasons
        contentType
        content
    }
}
```
A response with both a failed report a successful report may look like the following.

```json
{
  "data": {
    "getReports": [
      {
        "id": "42b1a3a9-77d6-4436-87fd-a0f07de[SchemaHelper.kt](..%2F..%2F..%2F..%2F..%2FDownloads%2Fapollo-kotlin-main%2Flibraries%2Fapollo-tooling%2Fsrc%2Fmain%2Fkotlin%2Fcom%2Fapollographql%2Fapollo%2Ftooling%2FSchemaHelper.kt)8e861",
        "uploadId": "e4361c73-348b-46f2-aad8-3043f8922f1d",
        "dataStreamId": "dex-testing",
        "dataStreamRoute": "test-event1",
        "messageId": null,
        "reportId": "42b1a3a9-77d6-4436-87fd-a0f07de8e861",
        "stageName": null,
        "status": null,
        "timestamp": "2024-05-25T17:47:24.360Z",
        "contentType": "application/json",
        "content": {
          "schema_version": "0.0.1",
          "metadata": {
            "meta_field1": "value1"
          },
          "filename": "some_upload1.csv",
          "schema_name": "dex-metadata-verify",
          "issues": null
        }
      }
    ],
    "getDeadLetterReportsByUploadId": [
      {
        "id": "392c0eed-4401-494a-8d38-f00a7431ca0f",
        "uploadId": "e4361c73-348b-46f2-aad8-3043f8922f1d",
        "dataStreamId": "dex-testing",
        "dataStreamRoute": null,
        "dispositionType": "ADD",
        "messageId": null,
        "reportId": "392c0eed-4401-494a-8d38-f00a7431ca0f",
        "stageName": null,
        "status": null,
        "timestamp": "2024-07-24T22:08:08.568Z",
        "deadLetterReasons": [
          "$.data_stream_route: is missing but it is required",
          "JSON is invalid against the content schema base.1.0.0.schema.json.Filename(s) used for validation: base.1.0.0.schema.json"
        ],
        "validationSchemas": [
          "base.1.0.0.schema.json"
        ],
        "contentType": "application/json",
        "content": {
          "report": {
            "ingested_file_path": "https://ocioedemessagesatst.blob.core.windows.net/hl7ingress/dex-routing/dex-smoke-test_319101ba2fd7835983f3257713819f7b",
            "ingested_file_timestamp": "2024-07-10T15:40:09+00:00",
            "ingested_file_size": 10240,
            "received_filename": "dex-smoke-test",
            "supporting_metadata": {
              "meta_ext_source": "test-src",
              "meta_ext_filestatus": "test-file-status",
              "meta_ext_file_timestamp": "test-timestamp",
              "system_provider": "DEX-ROUTING",
              "meta_ext_uploadid": "test-upload-id",
              "meta_ext_objectkey": "test-obj-key",
              "reporting_jurisdiction": "unknown"
            },
            "aggregation": "SINGLE",
            "number_of_messages": 1,
            "number_of_messages_not_propagated": 1,
            "error_messages": [
              {
                "message_uuid": "378d6660-903f-40b8-b48b-80f9d77aa69c",
                "message_index": 1,
                "error_message": "No valid message found."
              }
            ]
          },
          "content_schema_name": "hl7v2-debatch",
          "content_schema_version": "1.0.0"
        }
      }
    ]
  }
}
```
