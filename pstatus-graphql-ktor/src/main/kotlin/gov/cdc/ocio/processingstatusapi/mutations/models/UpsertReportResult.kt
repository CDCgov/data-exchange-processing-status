package gov.cdc.ocio.processingstatusapi.mutations.models

import com.expediagroup.graphql.generator.annotations.GraphQLDescription

/**
 * GraphQL result structure when attempting to upsert a report.
 *
 * @property result String?
 * @property reason String?
 * @property uploadId String?
 * @property reportId String?
 * @property issues List<String>?
 * @property schemaFileNames List<String>?
 * @constructor
 */
data class UpsertReportResult(

    @GraphQLDescription("Result of the operation, which can be SUCCESS or FAILURE.")
    var result: String? = null,

    @GraphQLDescription("If the result is FAILURE, this provides the high-level reason for the failure.")
    var reason: String? = null,

    @GraphQLDescription("If the result is SUCCESS, the uploadId of the report created or replaced.")
    var uploadId: String? =  null,

    @GraphQLDescription("If the result is SUCCESS, the reportId of the report created or replaced.")
    var reportId: String? = null,

    @GraphQLDescription("If the result is FAILURE, the detailed list of issues for why the report operation failed.")
    var issues: List<String>? = null,

    @GraphQLDescription("List of schema files used to validate the provided report.")
    var schemaFileNames: List<String>? = null
)