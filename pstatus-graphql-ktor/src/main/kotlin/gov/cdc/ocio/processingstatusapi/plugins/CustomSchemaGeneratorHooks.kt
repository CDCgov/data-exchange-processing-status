package gov.cdc.ocio.processingstatusapi.plugins

import com.expediagroup.graphql.generator.hooks.SchemaGeneratorHooks
import com.google.gson.JsonObject
import graphql.scalars.ExtendedScalars
import graphql.schema.GraphQLScalarType
import graphql.schema.GraphQLType
import java.time.OffsetDateTime
import kotlin.reflect.KClass
import kotlin.reflect.KType

val graphqlDateTimeClassType: GraphQLScalarType = GraphQLScalarType.newScalar()
    .name("DateTime")
    .coercing(ExtendedScalars.DateTime.coercing)
    .build()

val graphqlLongClassType: GraphQLScalarType = GraphQLScalarType.newScalar()
    .name("Long")
    .coercing(ExtendedScalars.GraphQLLong.coercing)
    .build()

val graphqlJsonObjectClassType: GraphQLScalarType = GraphQLScalarType.newScalar()
    .name("JsonObject")
    .coercing(ExtendedScalars.Json.coercing)
    .build()

class CustomSchemaGeneratorHooks : SchemaGeneratorHooks {
    override fun willGenerateGraphQLType(type: KType): GraphQLType? = when (type.classifier as? KClass<*>) {
        OffsetDateTime::class -> graphqlDateTimeClassType
        Long::class -> graphqlLongClassType
        JsonObject::class -> graphqlJsonObjectClassType
        else -> null
    }
}