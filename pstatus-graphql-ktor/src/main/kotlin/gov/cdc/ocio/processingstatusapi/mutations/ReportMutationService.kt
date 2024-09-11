package gov.cdc.ocio.processingstatusapi.mutations

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import gov.cdc.ocio.processingstatusapi.models.reports.ReportInput

@GraphQLDescription("A Mutation Service to either create a new report or replace an existing report")
class ReportMutationService() : Mutation {

    @GraphQLDescription("Create upload")
    @Suppress("unused")
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
        action: String,
        ) = ReportMutation().upsertReport(input, action)


}
