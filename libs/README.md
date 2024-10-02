# Processing Status Libraries
The processing status libraries are currently only intended for internal use within the collection of processing status services.  A number of the services share common features including database usage, messaging systems, validations, and more.  Rather than duplicate this code across the services the common functionality is provided in the libraries found here. 

Please see the readme in each of the subfolders for the details of each library.

## commons-database
The `commons-database` library is an interface for interacting with cloud and local databases.  With a common database interface you can have high-level code that works for all the supported databases.  Supported databases include cosmosdb, dynamodb, mongodb, and couchbase.