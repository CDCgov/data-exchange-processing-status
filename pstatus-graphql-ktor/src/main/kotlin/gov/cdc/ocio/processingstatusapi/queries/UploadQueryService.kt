package gov.cdc.ocio.processingstatusapi.queries

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query
import gov.cdc.ocio.processingstatusapi.loaders.UploadStatsLoader
import gov.cdc.ocio.processingstatusapi.loaders.UploadStatusLoader

class UploadQueryService : Query {

    /**
     * Get the upload status for the given search criteria.
     *
     * @param dataStreamId String
     * @param dataStreamRoute String?
     * @param dateStart String?
     * @param dateEnd String?
     * @param pageSize Int
     * @param pageNumber Int
     * @param sortBy String?
     * @param sortOrder String?
     * @return UploadsStatus
     */
    @GraphQLDescription("Get the upload statuses for the given filter, sort, and pagination criteria")
    @Suppress("unused")
    fun uploads(dataStreamId: String,
                dataStreamRoute: String? = null,
                dateStart: String? = null,
                dateEnd: String? = null,
                pageSize: Int = UploadStatusLoader.DEFAULT_PAGE_SIZE,
                pageNumber: Int = 1,
                sortBy: String? = null,
                sortOrder: String?) = UploadStatusLoader().uploadStatus(
        dataStreamId,
        dataStreamRoute,
        dateStart,
        dateEnd,
        pageSize,
        pageNumber,
        sortBy,
        sortOrder)

    /**
     * Provide the upload statistics for the given search criteria.
     *
     * @param dataStreamId String
     * @param dataStreamRoute String
     * @param dateStart String?
     * @param dateEnd String?
     * @param daysInterval Int?
     * @return UploadStats
     */
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
