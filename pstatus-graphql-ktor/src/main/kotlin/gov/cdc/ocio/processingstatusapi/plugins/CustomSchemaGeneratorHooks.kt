package gov.cdc.ocio.processingstatusapi.plugins

import com.expediagroup.graphql.generator.hooks.SchemaGeneratorHooks
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import graphql.language.*
import graphql.scalars.ExtendedScalars
import graphql.scalars.datetime.DateTimeScalar
import graphql.schema.*
import java.time.OffsetDateTime
import kotlin.math.absoluteValue
import kotlin.reflect.KClass
import kotlin.reflect.KType


fun objectValueToHashMap(objectValue: ObjectValue, objectMapper: ObjectMapper): CustomHashMap<String, Any?> {
    val jsonNode = objectValueToJsonNode(objectValue, objectMapper)
    return jsonNodeToHashMap(jsonNode)
}

fun jsonNodeToHashMap(jsonNode: JsonNode): CustomHashMap<String, Any?> {
    val hashMap = CustomHashMap<String, Any?>()

    jsonNode.fields().forEach { (key, value) ->
        hashMap.put(key, when {
            value.isObject -> jsonNodeToHashMap(value) // Recursive call for nested objects
            value.isArray -> value.map { node ->
                if (node.isObject) jsonNodeToHashMap(node) else node.asText()
            }
            value.isDouble -> value.doubleValue()
            value.isFloat -> value.floatValue()
            value.isBoolean -> value.asBoolean()
            value.isBigInteger -> value.asLong()
            value.isInt -> value.intValue()
            value.isLong -> value.longValue()
            value.isNull -> null
            else -> value.asText()
        })
    }

    return hashMap
}

fun objectValueToJsonNode(objectValue: ObjectValue, objectMapper: ObjectMapper): JsonNode {
    val objectNode = objectMapper.createObjectNode()

    objectValue.objectFields.forEach { field ->
        val key = field.name
        val value = convertValue(field.value, objectMapper)
        objectNode.set<JsonNode>(key, value)
    }

    return objectNode
}

private fun convertValue(value: Value<*>, objectMapper: ObjectMapper): JsonNode {
    return when (value) {
        is StringValue -> JsonNodeFactory.instance.textNode(value.value)
        is IntValue -> JsonNodeFactory.instance.numberNode(value.value)
        is FloatValue -> JsonNodeFactory.instance.numberNode(value.value)
        is BooleanValue -> JsonNodeFactory.instance.booleanNode(value.isValue)
        is EnumValue -> JsonNodeFactory.instance.textNode(value.name)
        is NullValue -> JsonNodeFactory.instance.nullNode()
        is ArrayValue -> {
            val arrayNode = JsonNodeFactory.instance.arrayNode()// objectMapper.createArrayNode()
            value.values.forEach { arrayNode.add(convertValue(it, objectMapper)) }
            arrayNode
        }
        is ObjectValue -> objectValueToJsonNode(value, objectMapper)
        else -> throw IllegalArgumentException("Unsupported GraphQL value type: ${value.javaClass}")
    }
}

val customHashMapScalar: GraphQLScalarType = GraphQLScalarType.newScalar()
    .name("customHashMapScalar")
    .description("A custom hash map scalar")
    .coercing(object: Coercing<CustomHashMap<String, Any?>, JsonNode> {
        private val mapper = ObjectMapper()
        @Deprecated("Deprecated in Java")
        override fun parseLiteral(input: Any): CustomHashMap<String, Any?> {
            if (input is ObjectValue) {
                return objectValueToHashMap(input, mapper)
            } else {
                throw CoercingParseLiteralException("Expected a StringValue")
            }
        }
    })
    .build()

/**
 * Define a GraphQL scalar for the Long type.
 */
val graphqlLongClassType: GraphQLScalarType = GraphQLScalarType.newScalar()
    .name("Long")
    .coercing(ExtendedScalars.GraphQLLong.coercing)
    .build()

/**
 * Custom schema generator hooks for the PS API reports.
 */
class CustomSchemaGeneratorHooks : SchemaGeneratorHooks {

    /**
     * Override the graphql schema generator types to include the new scalars for Long, DateTime, Maps, etc.
     *
     * @param type KType
     * @return GraphQLType?
     */
    override fun willGenerateGraphQLType(type: KType): GraphQLType? = when (type.classifier as? KClass<*>) {
        OffsetDateTime::class -> DateTimeScalar.INSTANCE
        Long::class -> graphqlLongClassType
        Map::class -> ExtendedScalars.Json
        CustomHashMap::class -> customHashMapScalar
        else -> null
    }

    override fun didGenerateGraphQLType(type: KType, generatedType: GraphQLType): GraphQLType {
        println("didGenerateGraphQLType: type = $type, generatedType = $generatedType ")
        return generatedType
    }

}

class CustomHashMap<K, V> {
    private data class Entry<K, V>(val key: K, var value: V)

    private val buckets = Array<MutableList<Entry<K, V>>?>(16) { null }
    private var size = 0

    private fun getBucketIndex(key: K): Int {
        return key.hashCode().absoluteValue % buckets.size
    }

    fun put(key: K, value: V) {
        val index = getBucketIndex(key)
        val bucket = buckets[index] ?: mutableListOf<Entry<K, V>>().also { buckets[index] = it }

        // Check if the key already exists
        for (entry in bucket) {
            if (entry.key == key) {
                entry.value = value // Update existing value
                return
            }
        }

        // Add new entry
        bucket.add(Entry(key, value))
        size++
    }

    fun get(key: K): V? {
        val index = getBucketIndex(key)
        val bucket = buckets[index] ?: return null

        for (entry in bucket) {
            if (entry.key == key) {
                return entry.value
            }
        }
        return null // Key not found
    }

    fun remove(key: K): Boolean {
        val index = getBucketIndex(key)
        val bucket = buckets[index] ?: return false

        val iterator = bucket.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.key == key) {
                iterator.remove()
                size--
                return true
            }
        }
        return false // Key not found
    }

    fun containsKey(key: K): Boolean {
        return get(key) != null
    }

    fun size(): Int {
        return size
    }

    fun isEmpty(): Boolean {
        return size == 0
    }

    fun toHashMap(): Map<K, V> {
        val hashmap = mutableMapOf<K, V>()
        buckets.forEach { bucket ->
            if (bucket != null) {
                for (entry in bucket) {
                    when (entry.value) {
                        is CustomHashMap<*, *> -> {
                            val copy = entry.value as CustomHashMap<*, *>
                            val valueMap = copy.toHashMap()
                            hashmap[entry.key] = valueMap as V
                        }
                        else -> hashmap[entry.key] = entry.value
                    }
                }
            }
        }
        return hashmap
    }
}
