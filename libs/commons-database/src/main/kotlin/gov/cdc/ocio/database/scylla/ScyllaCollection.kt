package gov.cdc.ocio.database.scylla

import com.datastax.driver.core.Session
import com.datastax.driver.mapping.MappingManager
import com.google.gson.*
import gov.cdc.ocio.database.models.Report
import gov.cdc.ocio.database.persistence.Collection
import java.util.*

import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import com.datastax.oss.driver.api.querybuilder.SchemaBuilder.*
import java.time.Instant


fun <T : Any> generateCqlSchema(modelClass: KClass<T>, tableName: String): String {
    val fields = modelClass.memberProperties.map { prop ->
        val columnName = prop.name
        val columnType = when (prop.returnType.classifier) {
            String::class -> "text"
            Int::class -> "int"
            Instant::class -> "bigint"
            Long::class -> "bigint"
            Boolean::class -> "boolean"
            Double::class -> "double"
            List::class -> "list<text>" // Adjust generics if needed
            Map::class -> "map<text, text>"
            Set::class -> "set<text>"
            Any::class -> "map<text, text>"
            else -> throw IllegalArgumentException("Unsupported type: ${prop.returnType}")
        }
        "$columnName $columnType"
    }.joinToString(",\n    ")

    return """
        CREATE TABLE IF NOT EXISTS test.$tableName (
            $fields,
            PRIMARY KEY (id)
        );
    """.trimIndent()
}

/**
 * Scylla Collection implementation.
 *
 * @param collectionName[String] Collection name associated with this couchbase collection.
 * @property couchbaseScope[Scope] Scope for the couchbase collection, which is defined by the bucket and collection name.
 * @property couchbaseCollection[com.couchbase.client.java.Collection] Couchbase collection associated with this collection.
 * @constructor Creates a couchbase collection for use with the [Collection] interface.
 *
 * @see [CouchbaseRepository]
 * @see [Collection]
 */
class ScyllaCollection(
    private val session: Session,
    private val keyspaceName: String,
    collectionName: String
): Collection {

    init {
//        session.execute("CREATE TABLE IF NOT EXISTS test.reports (id int PRIMARY KEY, name text);")
        val cqlSchema = generateCqlSchema(Report::class, "reports")
        session.execute(cqlSchema)
    }

    /**
     * Execute the provided query and return the results as POJOs.
     *
     * @param query[String]
     * @param classType Class<T>?
     * @return List<T>
     */
    override fun <T> queryItems(query: String?, classType: Class<T>?): List<T> {
        val mappingManager = MappingManager(session)
        val objectMapper = mappingManager.mapper(classType)
        val resultSet = session.execute(query)
        val results = objectMapper.map(resultSet).all()
        return results
    }

    /**
     * Create an item from the provided data.
     *
     * @param id String
     * @param item T
     * @param classType Class<T>
     * @param partitionKey String?
     * @return Boolean
     */
    override fun <T> createItem(id: String, item: T, classType: Class<T>, partitionKey: String?): Boolean {
//        val mapperFactory = ReportMapperFactoryBuilder(session).build()
        val result = runCatching {
            val mappingManager = MappingManager(session)
            val objectMapper = mappingManager.mapper(classType, keyspaceName)
            objectMapper.save(item)
        }
        return result.isSuccess
    }

    /**
     * Delete the specified item from the container.
     *
     * @param itemId String?
     * @param partitionKey String?
     * @return Boolean
     */
    override fun deleteItem(itemId: String?, partitionKey: String?): Boolean {
//        val removeResult = couchbaseCollection.remove(itemId)
//        return removeResult != null
        return true
    }

    override val collectionVariable = "r"

    override val collectionVariablePrefix = "r."

    override val openBracketChar = '['

    override val closeBracketChar = ']'

    override val collectionNameForQuery = "$keyspaceName.$collectionName"

    override val collectionElementForQuery = { name: String -> name }

}