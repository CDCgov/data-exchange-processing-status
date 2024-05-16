package gov.cdc.ocio.processingstatusapi

import com.expediagroup.graphql.generator.hooks.SchemaGeneratorHooks
import graphql.language.StringValue
import graphql.schema.Coercing
import graphql.schema.GraphQLScalarType
import graphql.schema.GraphQLType
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KType

//class CustomSchemaGeneratorHooks : SchemaGeneratorHooks {
//
//    override fun willGenerateGraphQLType(type: KType): GraphQLType? = when (type.classifier as? KClass<*>) {
//        Date::class -> graphqlDateType
//        else -> null
//    }
//}
//
//val graphqlDateType = GraphQLScalarType.newScalar()
//    .name("Date")
//    .description("A type representing a formatted java.util.UUID")
//    .coercing(DateCoercing)
//    .build()
//
//object DateCoercing : Coercing<Date, String> {
//    override fun parseValue(input: Any?): Date = Date.fromString(serialize(input))
//
//    override fun parseLiteral(input: Any?): UUID? {
//        val dateString = (input as? StringValue)?.value
//        return Date.fromString(dateString)
//    }
//
//    override fun serialize(dataFetcherResult: Any?): String = dataFetcherResult.toString()
//}
