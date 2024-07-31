# Overview
Reports are an essential component of the data observability aspect of the CDC Data Exchange (DEX).  In DEX, data is ingested to the system typically through a file upload.  As the upload progresses through the service line processing occurs.  The processing in the service line is made up stages, which can be the upload, routing, data validation, data transformations, etc.  Within each of those stages one or more actions may occur.  Taking the example of upload, one action within the stage may be to first verify that all the required metadata associated with the uploaded file is provided and reject it if not.  Other upload actions may include the file upload itself or the disposition of the upload for further downstream processing.  Reports are provided by both services internal to DEX and downstream of DEX as data moves through CDC systems.  Those services indicate the processing status of these stages through Reports.

## Report Sinking
This project is the processing status report sink.  It listens for messages on an Azure Service bus queues and topics, validates the messages, and if validated persists them to CosmosDB. If the validation fails due to missing fields or malformed data, then the message is persisted in cosmosdb under a new dead-letter container and the message is also sent to the dead-letter queue under the configured topic subscription(if the message was processed using the topic listener) 

This is a microservice built using Ktor that can be built as a docker container image.

# Publish to CDC's ImageHub
With one gradle command you can builds and publish the project's Docker container image to the external container registry, imagehub, which is a nexus repository.

To do this, we use Google [jib](https://cloud.google.com/java/getting-started/jib), which vastly simplifies the build process as you don't need a docker daemon running in order to build the Docker container image.

Inside of build.gradle `jib` section are the authentication settings.  These use environment variables for the username and password, which are `IMAGEHUB_USERNAME` and `IMAGEHUB_PASSWORD` respectively.
```commandline
gradle jib
```
The location of the deployment will be to the `docker-dev2` repository under the folder `/v2/dex/pstatus`.

# Report Delivery Mechanisms
Reports may be provided in one of two ways - either through calls into the Processing Status (PS) API as GraphQL mutations or by way of an Azure Service Bus.  There are pros and cons of each summarized below.

| Azure Service Bus   | GraphQL                  |
|---------------------|--------------------------|
| Fire and forget [1] | Confirmation of delivery |
| Fast                | Slower                   |

[1] Failed reports are sent to a Report deadletter that can be queried to find out the reason(s) for its rejection.  When using ASB there is no direct feedback mechanism to the report provider of the rejection.

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
