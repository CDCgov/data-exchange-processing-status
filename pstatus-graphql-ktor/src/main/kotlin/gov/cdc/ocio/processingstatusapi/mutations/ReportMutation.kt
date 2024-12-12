package gov.cdc.ocio.processingstatusapi.mutations

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import gov.cdc.ocio.processingstatusapi.exceptions.BadRequestException
import gov.cdc.ocio.processingstatusapi.exceptions.ContentException
import gov.cdc.ocio.processingstatusapi.models.reports.inputs.ReportInput
import gov.cdc.ocio.processingstatusapi.services.ReportMutationService

/**
 * ReportMutationService class handles GraphQL mutations for report creation and replacement.
 *
 * This service provides a single mutation operation to either create a new report or replace an
 * existing report in the system. It utilizes the ReportMutation class to perform the actual
 * upsert operation based on the provided input and action.
 *
 * Annotations:
 * - GraphQLDescription: Provides descriptions for the class and its methods for GraphQL documentation.
 *
 * Dependencies:
 * - ReportInput: Represents the input model for report data.
 */
@GraphQLDescription("A Mutation Service to either create a new report or replace an existing report")
class ReportMutation() : Mutation {

    /**
     * Creates a new report or updates existing report based on action.
     *
     * This function serves as a GraphQL mutation to create a new report or replace an existing one.
     * It delegates the actual upsert logic to the ReportMutation class.
     *
     * @param input The report to be created or updated.
     * @param action A string specifying the action to perform: "create" or "replace".
     * @return The result of the upsert operation, handled by the ReportMutation class.
     */
    @GraphQLDescription("Creates a new report or replace an existing one based on action specified.")
    @Suppress("unused")
    @Throws(BadRequestException::class, ContentException::class, Exception::class)
    fun upsertReport( @GraphQLDescription(
        "*Action*: Can be one of the following values\n"
                + "`create`: Creates a new report.\n"
                + "`replace`: Replace an existing report.\n"
        )
        action: String,

        @GraphQLDescription(
            "*Report* to be created or updated.\n"
        )
        report: ReportInput
        ) = ReportMutationService().upsertReport(action, report)
}
