# commons-database
The `commons-database` library is an interface for interacting with cloud and local databases.  With a common database interface you can have high-level code that works for all the supported databases.

## Supported Databases
Supported databases include cosmosdb, dynamodb, mongodb, and couchbase.

> **_Note:_** The mongodb implementation is currently only partially supported.  The `commons-library` can be used to write and delete mongodb objects, but the query interface is not implemented due to the need for Atlas to do SQL-like queries with mongodb.

## Usage

### gradle
Add the following to the `dependencies` of your project' `build.gradle`.
```groovy
dependencies {
    implementation project(':libs:commons-database')
}
```
This will allow the `commons-library` to be compiled if necessary and linked with your project.  You can set breakpoints in the library the same as you would your main project for debugging.

> **_Important:_** You can't have a `settings.gradle` file in your project.  Delete it if you have one.  Otherwise, the root project `settings.gradle` will not be picked up and gradle won't be able to find the library. 

### ktor
The following excerpt of code describes how the library can be used with ktor and koin.  Koin is a common ktor library used for dependency injection.

```kotlin
fun KoinApplication.loadKoinModules(environment: ApplicationEnvironment): KoinApplication {
    val databaseModule = module {
        when (val database = environment.config.tryGetString("ktor.database")) {
            DatabaseType.MONGO.value -> {
                val connectionString = environment.config.property("mongo.connection_string").getString()
                single<ProcessingStatusRepository>(createdAtStart = true) {
                    MongoRepository(connectionString, "ProcessingStatus")
                }
            }
            DatabaseType.COUCHBASE.value -> {
                val connectionString = environment.config.property("couchbase.connection_string").getString()
                single<ProcessingStatusRepository>(createdAtStart = true) {
                    CouchbaseRepository(connectionString, "admin", "password")
                }
            }
            DatabaseType.COSMOS.value -> {
                val uri = environment.config.property("azure.cosmos_db.client.endpoint").getString()
                val authKey = environment.config.property("azure.cosmos_db.client.key").getString()
                single<ProcessingStatusRepository>(createdAtStart = true) {
                    CosmosRepository(uri, authKey, "/uploadId")
                }
            }
            DatabaseType.DYNAMO.value -> {
                val dynamoTablePrefix = environment.config.property("dynamo.table_prefix").getString()
                single<ProcessingStatusRepository>(createdAtStart = true) {
                    DynamoRepository(dynamoTablePrefix)
                }
            }
            else -> logger.error("Unsupported database requested: $database")
        }
    }
    return modules(listOf(databaseModule))
}
```

You can now use this in your service as follows.
```kotlin
class MyService: KoinComponent {
    
    private val repository by inject<ProcessingStatusRepository>()

    fun writeReportExample() {
        val success = repository.reportsCollection.createItem(
            reportId,
            reportType,
            Report::class.java,
            uploadId
        )
    }
    
    fun deleteReportExample() {
        repository.reportsCollection.deleteItem(
            it.id,
            it.uploadId
        )
    }
    
    fun queryReportsExample() {
        val cName = repository.reportsCollection.collectionNameForQuery
        val cVar = repository.reportsCollection.collectionVariable
        val cPrefix = repository.reportsCollection.collectionVariablePrefix
        val cElFunc = repository.reportsCollection.collectionElementForQuery
        val sqlQuery = (
                "select * from $cName $cVar "
                        + "where ${cPrefix}uploadId = '$uploadId' "
                        + "and ${cPrefix}stageInfo.${cElFunc("service")} = '${stageInfo?.service}' "
                        + "and ${cPrefix}stageInfo.${cElFunc("action")} = '${stageInfo?.action}'"
                )
        val items = repository.reportsCollection.queryItems(
            sqlQuery,
            Report::class.java
        )
    }
}
```