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
        Long::class ->graphqlLongClassType
        Map::class -> JsonScalar.INSTANCE
        else -> null
    }
}