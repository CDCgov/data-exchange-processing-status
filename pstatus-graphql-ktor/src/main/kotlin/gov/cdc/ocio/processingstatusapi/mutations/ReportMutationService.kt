package gov.cdc.ocio.processingstatusapi.mutations

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import com.fasterxml.jackson.databind.ObjectMapper
import gov.cdc.ocio.processingstatusapi.exceptions.BadRequestException
import gov.cdc.ocio.processingstatusapi.exceptions.ContentException
import gov.cdc.ocio.processingstatusapi.models.Report
import gov.cdc.ocio.processingstatusapi.models.reports.inputs.ReportInput

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
class ReportMutationService() : Mutation {

    /**
     * Upserts a report based on the provided input and action.
     *
     * This function serves as a GraphQL mutation to create a new report or replace an existing one.
     * It delegates the actual upsert logic to the ReportMutation class.
     *
     * @param input The ReportInput containing details of the report to be created or replaced.
     * @param action A string specifying the action to perform: "create" or "replace".
     * @return The result of the upsert operation, handled by the ReportMutation class.
     */
    @GraphQLDescription("Create upload")
    @Suppress("unused")
    @Throws(BadRequestException::class, ContentException::class, Exception::class)
    fun upsertReport(
        @GraphQLDescription(
            "*Report Input* to be created or updated:\n"
        )
        input: ReportInput,
        @GraphQLDescription(
            "*Action*: Can be one of the following values\n"
                    + "`create`: Create new report\n"
                    + "`replace`: Replace existing report\n"
        )
        action: String
        ) : Report? {
        return try {
            ReportMutation().upsertReport(input, action)
        } catch (e: BadRequestException) {
            throw e // Rethrow to inform the GraphQL layer
        } catch (e: ContentException) {
            throw e // Rethrow to inform the GraphQL layer
        } catch (e: Exception) {
            throw e // Rethrow to inform the GraphQL layer
        }
    }


}
