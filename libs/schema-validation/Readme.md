# schema-validation
The `schema-validation` library is used for processing status report validations. Each report has a schema associated with it that this library can be used to determine whether the report is valid or not and if not, the reasons why. This will contain the interfaces and their implementations, which can be reused across multiple services.

# Structure Overview  

## Interfaces:

### SchemaLoader: 
Interface to load schema files.
### SchemaValidator: 
Interface to validate JSON data against schemas.
### ErrorProcessor: 
Interface for handling errors.
### JsonUtils: 
Utility interface for JSON operations (parsing, MIME type checks, etc.).

## Classes 
### SchemaValidationService: 
Core service that uses the above interfaces to perform validation and processing.
### CloudSchemaLoader
A loader that retrieves schema files from cloud storage solutions like S3 or Azure Blob Storage.
### FileSchemaLoader: 
Loads schema files from the classpath.
### JsonSchemaValidator: 
Validates JSON against schemas.
### ErrorLoggerProcessor: 
Handles logging and sending errors to dead-letter queues.
### DefaultJsonUtils: 
Utility methods for JSON operations (e.g., MIME type validation).
## Usage

### gradle
Add the following to the `dependencies` of your project' `build.gradle`.
```groovy
dependencies {
    implementation project(':libs:schema-validation')
}
```
This will allow the `schema-validation` to be compiled if necessary and linked with your project.  You can set breakpoints in the library the same as you would your main project for debugging.

### ktor
You can now use this in your report-sink-ktor service as follows.

```kotlin
fun validateJsonSchema(message: String, source: Source) {
    val malformedJson = "Report rejected: Malformed JSON or error processing the report"
    val objectMapper: ObjectMapper = jacksonObjectMapper()
    val logger: KLogger = mock(KLogger::class.java)
    val errorProcessor: ErrorProcessor = ErrorLoggerProcessor(logger)
    val schemaValidator: SchemaValidator = JsonSchemaValidator(logger)
    val jsonUtils: JsonUtils = DefaultJsonUtils(objectMapper)
    val schemaLoader: SchemaLoader = FileSchemaLoader()
    val createReportMessage: CreateReportMessage
    val schemaValidationService = SchemaValidationService(
        schemaLoader,
        schemaValidator,
        errorProcessor,
        jsonUtils,
        logger
    )
    try {
        createReportMessage = gson.fromJson(message, CreateReportMessage::class.java)
        //createReportMessage.source= source
        val result: ValidationSchemaResult = schemaValidationService.validateJsonSchema(message)
        var isBadRequest = false
        if (!result.status) {
            val invalidData = result.invalidData
            if (invalidData.any { it == malformedJson }) {
                val malformedReportMessage = safeParseMessageAsReport(message)
                sendToDeadLetter(invalidData, result.schemaFileNames, malformedReportMessage)
            } else {
                isBadRequest = true
                logger.error("The report validation failed ${result.reason}")
            }
            sendToDeadLetter(invalidData, result.schemaFileNames, createReportMessage)
            if (isBadRequest) throw BadRequestException(invalidData.joinToString(separator = ","))

        }

    }
    //This is for unhandled exception coming from the schema validation library
    catch (e: Exception) {
        logger.error("The schema validation library threw an error {e.message}")
        throw e
    }
} 

```
### Cloud Schema Loader Usage
In order to use the Cloud Schema Loader from report-sink service or any other service, please follow these steps
```kotlin
Usage for S3
------------
Env Vars for S3
---------------
You need to specify these 2 in the configs or env vars 
a) Bucket Name
b) Region

REPORT_SCHEMA_S3_BUCKET ="dex-ps-api-reports-schema-bucket"
REPORT_SCHEMA_S3_REGION="us-east-1"

Sample code 
-----------
val config=mapOf(
"REPORT_SCHEMA_S3_BUCKET"to REPORT_SCHEMA_S3_BUCKET,
"REPORT_SCHEMA_S3_REGION"to REPORT_SCHEMA_S3_REGION
)
//for example to get the contents of base.0.0.1.schema.json
val fileName="base.0.0.1.schema.json"
val loader=CloudSchemaLoader("s3",config)
val schemaFile:SchemaFile=loader.loadSchemaFile(fileName)

Usage for Blob Storage
------------
Env Vars for Blob
---------------
You need to specify these 2 in the configs or env vars 
a) Blob Connection String
b) Container Name

REPORT_SCHEMA_BLOB_CONNECTION_STR = "DefaultEndpointsProtocol=https;AccountName=ocioedeprocstatusdev;AccountKey=xxxxxxx"
REPORT_SCHEMA_BLOB_CONTAINER="dex-ps-api-reports-schema"

//Make sure to get the AccountKey from azure blob storage(for dev- ocioedeprocstatusdev)
Sample code 
-----------
val config=mapOf(
"REPORT_SCHEMA_BLOB_CONNECTION_STR"to REPORT_SCHEMA_BLOB_CONNECTION_STR,
"REPORT_SCHEMA_BLOB_CONTAINER"to REPORT_SCHEMA_BLOB_CONTAINER
)

//for example to get the contents of base.0.0.1.schema.json
val fileName="base.0.0.1.schema.json"
val loader= CloudSchemaLoader("blob_storage",config)
val schemaFile:SchemaFile=loader.loadSchemaFile(fileName)
