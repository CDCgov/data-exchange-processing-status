# Processing Status Libraries
The processing status libraries are currently only intended for internal use within the collection of processing status services.  A number of the services share common features including database usage, messaging systems, validations, and more.  Rather than duplicate this code across the services the common functionality is provided in the libraries found here. 

Please see the readme in each of the subfolders for the details of each library.

## commons-database
The `commons-database` library is an interface for interacting with cloud and local databases.  With a common database interface you can have high-level code that works for all the supported databases.  Supported databases include cosmosdb, dynamodb, mongodb, and couchbase.

## rules-engine 
The `rules-engine` library allows business rules to be dynamically defined to drive actions.  The intended purpose is for use in workflows to specify conditions for state determination.

## schema-validation 
The `schema-validation` library is used for processing status report validations.  Each report has a schema associated with it that this library can be used to determine whether the report is valid or not and if not, the reasons why.

## subscription-management _(coming soon)_
The `subscriptions-management` library is used for managing notification subscriptions.  Notification subscriptions are setup by end-users to get automatically notified when conditions are met through a variety of delivery mechanisms.  This library will provide the means to define and manage those subscriptions.
