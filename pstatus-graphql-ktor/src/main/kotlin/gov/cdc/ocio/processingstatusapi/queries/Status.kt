package gov.cdc.ocio.processingstatusapi.queries

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query
import gov.cdc.ocio.processingstatusapi.loaders.StatusLoader

class StatusQueryService : Query {

    @GraphQLDescription("Return various uploads statistics")
    @Suppress("unused")
    fun getUploadStats(@GraphQLDescription("Data stream ID")
                       dataStreamId: String,
                       @GraphQLDescription("Data stream Route")
                       dataStreamRoute: String,
                       @GraphQLDescription("Start date of the included data.  dateStart or daysInterval is required.")
                       dateStart: String? = null,
                       @GraphQLDescription("End date of the search.  If not specified then all data up to now is included.")
                       dateEnd: String? = null,
                       @GraphQLDescription("Number of days to include in the search before today.  If 0, then search for today only.")
                       daysInterval: String? = null) =
        StatusLoader().getUploadStats(dataStreamId, dataStreamRoute, dateStart, dateEnd, daysInterval)
}
