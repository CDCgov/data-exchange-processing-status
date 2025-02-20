package gov.cdc.ocio.processingstatusapi.mutations

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import gov.cdc.ocio.processingstatusapi.collections.BasicHashMap
import gov.cdc.ocio.processingstatusapi.services.ReportSchemaMutationService
import graphql.schema.DataFetchingEnvironment


/**
 * SchemaMutation class handles GraphQL mutations for report schema management.
 *
 * This service provides a mutation operations to manage report schemas.  Management of schemas including adding,
 * updating or removing schemas.
 *
 * Annotations:
 * - GraphQLDescription: Provides descriptions for the class and its methods for GraphQL documentation.
 *
 */
@GraphQLDescription("A Mutation Service to manage report schemas, including adding, updating or removing schemas.")
class ReportSchemaMutation : Mutation {

    @GraphQLDescription("Upserts a report schema to be used by the Processing Status (PS) API for ingesting reports. "
            + "Report schemas are used to validate incoming reports before they are recorded.")
    @Suppress("unused")
    fun upsertSchema(
        dataFetchingEnvironment: DataFetchingEnvironment,
        @GraphQLDescription("Name of the report schema to upsert.")
        schemaName: String,
        @GraphQLDescription("Version of the report schema to upsert.")
        schemaVersion: String,
        @GraphQLDescription("Content of the report schema.  Note: The schema field names that begin with $ should have "
        + "the $ omitted from the name.  For example, the field '\$schema' should be 'schema' here in the content of "
        + "the report schema.")
        content: BasicHashMap<String, Any?>
    ) = run {
        val service = ReportSchemaMutationService()
        service.upsertSchema(dataFetchingEnvironment, schemaName, schemaVersion, content)
    }

    @GraphQLDescription("Removes a report schema.  If the report schema file associated with the provided schema "
            + "name and version is not found an error is returned.")
    @Suppress("unused")
    fun removeSchema(
        dataFetchingEnvironment: DataFetchingEnvironment,
        @GraphQLDescription("Name of the report schema to remove.")
        schemaName: String,
        @GraphQLDescription("Version of the report schema to upsert.")
        schemaVersion: String
    ) = run {
        val service = ReportSchemaMutationService()
        service.removeSchema(dataFetchingEnvironment, schemaName, schemaVersion)
    }
}
