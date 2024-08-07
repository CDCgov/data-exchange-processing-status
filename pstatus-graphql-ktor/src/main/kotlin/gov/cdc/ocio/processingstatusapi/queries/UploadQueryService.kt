package gov.cdc.ocio.processingstatusapi.queries

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query
import gov.cdc.ocio.processingstatusapi.loaders.UploadStatsLoader
import gov.cdc.ocio.processingstatusapi.loaders.UploadStatusLoader

@GraphQLDescription("A list of functions to query any information related to the uploads")
class UploadQueryService : Query {

    @GraphQLDescription("Get the upload statuses for the given filter, sort, and pagination criteria")
    @Suppress("unused")
    fun getUploads(
                @GraphQLDescription(
                    "*Data Stream Id* Search by the provided Data Stream Id:\n"
                )
                dataStreamId: String,

                @GraphQLDescription(
                   "*Data Stream Route* Search by the provided Data Stream Route:\n"
                )
                dataStreamRoute: String? = null,

                @GraphQLDescription(
                   "*Start Date* is a Date Time value. Sets the start date for the search results :\n"
                )
                dateStart: String? = null,

                @GraphQLDescription(
                   "*End Date* is a Date Time value. Sets the end date for the search results :\n"
                )
                dateEnd: String? = null,

                @GraphQLDescription(
                   "*Page Size* is the number of results to be fetched per page:\n"
                )
                pageSize: Int = UploadStatusLoader.DEFAULT_PAGE_SIZE,

                @GraphQLDescription(
                   "*Page Number* is specified to fetch the results associated with the respective page number:\n"
                )
                pageNumber: Int = 1,

                @GraphQLDescription(
                   "*Sort By* can be specified as any one of the following values:\n"
                           + "`fileName`: Sort By the fileName\n"
                           + "`date`: Sort By the date\n"
                           + "`dataStreamId`: Sort By the dataStreamId\n"
                           + "`dataStreamRoute`: Sort By the dataStreamRoute\n"
                           + "`stageName`: Sort By the stageName\n"
                           + "If a value is provided that is not supported then a bad request response is returned."
                )
                sortBy: String? = null,

                @GraphQLDescription(
                    "*Sort Order* can be specified as any one of the following values:\n"
                            + "`asc`: Ascending order\n"
                            + "`desc`: Descending order\n"
                            + "If a value is provided that is not supported then a bad request response is returned."
                )
                sortOrder: String?,

                @GraphQLDescription(
                    "*File Name* Search by the provided File Name:\n"
                )
                fileName: String? = null) =
        UploadStatusLoader().getUploadStatus(dataStreamId,
                                        dataStreamRoute,
                                        dateStart,
                                        dateEnd,
                                        pageSize,
                                        pageNumber,
                                        sortBy,
                                        sortOrder,
                                        fileName)

    @GraphQLDescription("Return various uploads statistics")
    @Suppress("unused")
    fun getUploadStats(@GraphQLDescription("Data stream ID")
                       dataStreamId: String,
                       @GraphQLDescription("Data stream route")
                       dataStreamRoute: String,
                       @GraphQLDescription("Start date of the included data.  dateStart or daysInterval is required.")
                       dateStart: String? = null,
                       @GraphQLDescription("End date of the search.  If not specified then all data up to now is included.")
                       dateEnd: String? = null,
                       @GraphQLDescription("Number of days to include in the search before today.  If 0, then search for today only.")
                       daysInterval: Int? = null) =
        UploadStatsLoader().getUploadStats(dataStreamId, dataStreamRoute, dateStart, dateEnd, daysInterval)
}

