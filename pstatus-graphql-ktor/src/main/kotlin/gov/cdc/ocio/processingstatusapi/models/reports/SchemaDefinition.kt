package gov.cdc.ocio.processingstatusapi.models.reports

import com.expediagroup.graphql.generator.annotations.GraphQLDescription

@GraphQLDescription("Report content schema name and version information.  All report content implement this interface.")
interface SchemaDefinition {

    @GraphQLDescription("Schema name for the report content")
    val schemaName: String

    @GraphQLDescription("Version of the schema for the report content")
    val schemaVersion: String
}