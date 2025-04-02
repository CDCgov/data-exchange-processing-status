# Processing Status Libraries
The processing status libraries are currently only intended for internal use within the collection of processing status services.  A number of the services share common features including database usage, messaging systems, validations, and more.  Rather than duplicate this code across the services the common functionality is provided in the libraries found here. 

Please see the readme in each of the subfolders for the details of each library.

## commons-database
The `commons-database` library is an interface for interacting with cloud and local databases.  With a common database interface you can have high-level code that works for all the supported databases.  Supported databases include cosmosdb, dynamodb, and couchbase.

## commons-types
The `commons-types` library is a collection of types including enumerations, models, and utility classes that are commonly shared by the processing status API services.

## message-system
The `message-system` library provides a system for sending and receiving messages on a queue or topic.  Current implementations include RabbitMQ, AWS SNS/SQS, and Azure Service Bus.

## schema-validation 
The `schema-validation` library is used for processing status report validations.  Each report has a schema associated with it that this library can be used to determine whether the report is valid or not and if not, the reasons why.
