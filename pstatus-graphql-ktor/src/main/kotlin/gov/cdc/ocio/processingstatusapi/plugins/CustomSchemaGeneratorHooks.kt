package gov.cdc.ocio.processingstatusapi.plugins

import com.expediagroup.graphql.generator.hooks.SchemaGeneratorHooks
import graphql.scalars.ExtendedScalars
import graphql.scalars.datetime.DateTimeScalar
import graphql.scalars.`object`.JsonScalar
import graphql.schema.GraphQLScalarType
import graphql.schema.GraphQLType
import java.time.OffsetDateTime
import kotlin.reflect.KClass
import kotlin.reflect.KType

val graphqlLongClassType: GraphQLScalarType = GraphQLScalarType.newScalar()
    .name("Long")
    .coercing(ExtendedScalars.GraphQLLong.coercing)
    .build()

class CustomSchemaGeneratorHooks : SchemaGeneratorHooks {
    override fun willGenerateGraphQLType(type: KType): GraphQLType? = when (type.classifier as? KClass<*>) {
        OffsetDateTime::class -> DateTimeScalar.INSTANCE
        Long::class ->graphqlLongClassType
        Map::class -> JsonScalar.INSTANCE
        else -> null
    }
}