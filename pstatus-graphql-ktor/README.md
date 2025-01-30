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
| processingCounts(..)                      | ProcessingCounts                    | Processing counts for the provided parameters.                                                                              |
| reportCountsWithParams(..)                | AggregateReportCounts               | Detailed counts within each stage for each matching upload with the provided parameters.                                    |
| reportCountsWithUploadId(..)              | ReportCounts                        | Detailed counts within each stage for the provided uploadId.                                                                |
| rollupCountsByStage(..)                   | list of stage counts                | Rolled up counts by stage for the provided parameters.                                                                      |
| getDeadLetterReportsByDataStream(..)      | list of dead letter reports         | All the dead-letter reports associated with the provided datastreamId, datastreamroute and timestamp date range.            |
| getDeadLetterReportsByUploadId(..)        | list of dead letter reports         | All the dead-letter reports associated with the provided uploadId.                                                          |
| getDeadLetterReportsCountByDataStream(..) | int, number of reports              | Count of dead-letter reports associated with the provided datastreamId, (optional) datastreamroute and timestamp date range |
| getUploadStats(..)                        | UploadStats                         | Various uploads statistics.                                                                                                 |
| getUploads(..)                            | UploadsStatus                       | Upload statuses for the given filter, sort, and pagination criteria.                                                        |




## Environment Variable Setup

### Database
The `DATABASE` environment variable is used to specify the database used for persisting reports. Supported databases are:
 - `cosmos`
 - `dynamo`
 - `couchbase`
 
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

For report schema loader, set the following environment variables:
- `REPORT_SCHEMA_LOADER_SYSTEM` - One of these values (s3, blob_storage or file_system)
- For `s3` -
- `REPORT_SCHEMA_S3_BUCKET` - S3 Bucket Name
- `REPORT_SCHEMA_S3_REGION` - S3 Region.
- For `blob_storage`
- `REPORT_SCHEMA_BLOB_CONNECTION_STR` - Connection string of the storage account.
- `REPORT_SCHEMA_BLOB_CONTAINER` - Blob container name.
-  For `file_system`
- `REPORT_SCHEMA_LOCAL_FILE_SYSTEM_PATH`- The local file system path where the reports schema reside

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

### LOKI Logging-  Docker Compose and other configs 

For supporting LOKI logging and visualization of the logs through Grafana(dashboard) we need to setup containers for LOKI, Promtail and Grafana. 
####
The LOKI does the logging part, the logs are pushed to LOKI through promtail and can be visualized through grafana dashboards using LOKI as the datasource. The docker compose in this service would install and mount the LOKI, Promtail and Grafana containers in the local docker instance.The corresponding logs files for each are stored in a directory named "var". These files are mounted as volumes under the corresponding services (loki and promtail). 
services:
``` Loki Service
loki:
image: grafana/loki:2.9.1
container_name: loki
ports:
- "3100:3100"
command: -config.file=/etc/loki/local-config.yaml
volumes:
- ./loki-config.yaml:/etc/loki/loki-config.yaml

# Promtail Service
promtail:
image: grafana/promtail:2.9.1
container_name: promtail
ports:
- "9080:9080"
volumes:
- /var/run/docker.sock:/var/run/docker.sock
- ./var/promtail-config.yaml:/etc/promtail/config.yaml
- ./reports:/reports # Bind mount the report schemas folder
command: -config.file=/etc/promtail/config.yaml
depends_on:
- loki
```
For Grafana installation, which is dependent on LOKI and Promtail, we need to setup a volume for grafana data 
```Grafana Service
grafana:
image: grafana/grafana:10.0.0
container_name: grafana
ports:
- "3000:3000"
environment:
- GF_SECURITY_ADMIN_USER=admin
- GF_SECURITY_ADMIN_PASSWORD=admin
volumes:
- grafana-data:/var/lib/grafana
depends_on:
- loki
- promtail

volumes:
grafana-data:
````

### Logback xml
LOKI logging needs a logback.xml file which needs to reside on the resources directory. The logback xml defines the type of appender and the encoder we need to use as well as any custom fields like the application name , environment etc..
####
Here we are using a STDOUT appender which logs to the console and another appender named LOKI which also logs to the console but using a JSON format.
The ENVIRONMENT variable below needs to be set in the application.conf where values would be Development, Staging & Production.If not set, the default would be Development.
````
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{YYYY-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    <appender name="LOKI" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder" />
        <customFields>
            {
            "application": "pstatus-api-graphql",
            "environment": "${ENVIRONMENT:-development}"
            }
        </customFields>

        <labels>
            <label name="job">kotlin-app</label>
            <label name="instance">instance1</label>
        </labels>
    </appender>
    <root level="INFO">
        <appender-ref ref="STDOUT"/>
        <appender-ref ref="LOKI"/>
    </root>
    <logger name="org.eclipse.jetty" level="INFO"/>
    <logger name="io.netty" level="INFO"/>
</configuration>
````