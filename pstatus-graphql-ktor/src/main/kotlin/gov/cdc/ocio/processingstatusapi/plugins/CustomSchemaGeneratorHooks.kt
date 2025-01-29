package gov.cdc.ocio.processingstatusapi.plugins

import com.expediagroup.graphql.generator.hooks.SchemaGeneratorHooks
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import gov.cdc.ocio.processingstatusapi.collections.BasicHashMap
import gov.cdc.ocio.types.health.HealthStatusType
import graphql.GraphQLContext
import graphql.execution.CoercedVariables
import graphql.language.*
import graphql.scalars.ExtendedScalars
import graphql.scalars.datetime.DateTimeScalar
import graphql.schema.*
import java.time.OffsetDateTime
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KType


/**
 * Converts an [ObjectValue] to a [BasicHashMap].
 *
 * @param objectValue ObjectValue
 * @return BasicHashMap<String, Any?>
 */
fun objectValueToHashMap(objectValue: ObjectValue): BasicHashMap<String, Any?> {
    val jsonNode = objectValueToJsonNode(objectValue)
    return jsonNodeToHashMap(jsonNode)
}

/**
 * Generates a [BasicHashMap] from a provided [JsonNode].
 *
 * @param jsonNode JsonNode
 * @return BasicHashMap<String, Any?>
 */
fun jsonNodeToHashMap(jsonNode: JsonNode): BasicHashMap<String, Any?> {
    val hashMap = BasicHashMap<String, Any?>()

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

/**
 * Converts an [ObjectValue] to a [JsonNode].
 *
 * @param objectValue ObjectValue
 * @return JsonNode
 */
fun objectValueToJsonNode(objectValue: ObjectValue): JsonNode {
    val objectNode = ObjectMapper().createObjectNode()

    objectValue.objectFields.forEach { field ->
        val key = field.name
        val value = convertValue(field.value)
        objectNode.set<JsonNode>(key, value)
    }

    return objectNode
}

/**
 * Converts a [Value] generic to a [JsonNode].
 *
 * @param value Value<*>
 * @return JsonNode
 */
private fun convertValue(value: Value<*>): JsonNode {
    return when (value) {
        is StringValue -> JsonNodeFactory.instance.textNode(value.value)
        is IntValue -> JsonNodeFactory.instance.numberNode(value.value)
        is FloatValue -> JsonNodeFactory.instance.numberNode(value.value)
        is BooleanValue -> JsonNodeFactory.instance.booleanNode(value.isValue)
        is EnumValue -> JsonNodeFactory.instance.textNode(value.name)
        is NullValue -> JsonNodeFactory.instance.nullNode()
        is ArrayValue -> {
            val arrayNode = JsonNodeFactory.instance.arrayNode()
            value.values.forEach { arrayNode.add(convertValue(it)) }
            arrayNode
        }
        is ObjectValue -> objectValueToJsonNode(value)
        else -> throw IllegalArgumentException("Unsupported GraphQL value type: ${value.javaClass}")
    }
}

/**
 * Implementation of a basic hash map scalar.
 */
val basicHashMapScalar: GraphQLScalarType = GraphQLScalarType.newScalar()
    .name("BasicHashMap")
    .description("A basic hash map scalar")
    .coercing(object : Coercing<BasicHashMap<String, Any?>, JsonNode> {
        /**
         * Coerces a [Value] generic to a [BasicHashMap].
         *
         * @param input Value<*>
         * @param variables CoercedVariables
         * @param graphQLContext GraphQLContext
         * @param locale Locale
         * @return BasicHashMap<String, Any?>
         */
        override fun parseLiteral(
            input: Value<*>,
            variables: CoercedVariables,
            graphQLContext: GraphQLContext,
            locale: Locale
        ): BasicHashMap<String, Any?> {
            if (input is ObjectValue) {
                return objectValueToHashMap(input)
            } else {
                throw CoercingParseLiteralException("Expected an ObjectValue")
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
 * Define a GraphQL scalar for the HealthStatus type.
 */
val healthStatusTypeScalar: GraphQLScalarType = GraphQLScalarType.newScalar()
    .name("HealthStatusType")
    .coercing(object : Coercing<HealthStatusType, String> {
        /**
         * Serialize implementation of the [HealthStatusType] scalar.
         *
         * @param dataFetcherResult Any
         * @param graphQLContext GraphQLContext
         * @param locale Locale
         * @return String
         */
        override fun serialize(dataFetcherResult: Any, graphQLContext: GraphQLContext, locale: Locale): String {
            if (dataFetcherResult is HealthStatusType)
                return dataFetcherResult.value
            else
                throw CoercingSerializeException("Expected a HealthStatusType")
        }
    })
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
        BasicHashMap::class -> basicHashMapScalar
        HealthStatusType::class -> healthStatusTypeScalar
        else -> null
    }

    override fun didGenerateGraphQLType(type: KType, generatedType: GraphQLType): GraphQLType {
        println("didGenerateGraphQLType: type = $type, generatedType = $generatedType ")
        return generatedType
    }

}
