package gov.cdc.ocio.processingstatusapi.models

import com.expediagroup.graphql.generator.annotations.GraphQLDescription


@GraphQLDescription("Result of a report schema management action, such as upserting or removing a report schema.")
data class SchemaActionResult(

    @GraphQLDescription("Report schema action that was performed.")
    val action: String,

    @GraphQLDescription("Result of the action.")
    val result: String,

    @GraphQLDescription("Report schema filename associated with this action.")
    val filename: String
)