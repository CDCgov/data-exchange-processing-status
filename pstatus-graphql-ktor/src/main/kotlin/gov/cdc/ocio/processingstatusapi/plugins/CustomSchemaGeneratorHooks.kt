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
import kotlin.reflect.KClass
import kotlin.reflect.KType


fun objectValueToHashMap(objectValue: ObjectValue): CustomHashMap<String, Any?> {
    val jsonNode = objectValueToJsonNode(objectValue)
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

fun objectValueToJsonNode(objectValue: ObjectValue): JsonNode {
    val objectNode = ObjectMapper().createObjectNode()

    objectValue.objectFields.forEach { field ->
        val key = field.name
        val value = convertValue(field.value)
        objectNode.set<JsonNode>(key, value)
    }

    return objectNode
}

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

val customHashMapScalar: GraphQLScalarType = GraphQLScalarType.newScalar()
    .name("customHashMapScalar")
    .description("A custom hash map scalar")
    .coercing(object: Coercing<CustomHashMap<String, Any?>, JsonNode> {
        @Deprecated("Deprecated in Java")
        override fun parseLiteral(input: Any): CustomHashMap<String, Any?> {
            if (input is ObjectValue) {
                return objectValueToHashMap(input)
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
