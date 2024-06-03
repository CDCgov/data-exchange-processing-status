package gov.cdc.ocio.processingstatusapi.queries

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query
import gov.cdc.ocio.processingstatusapi.loaders.ReportCountsLoader
import gov.cdc.ocio.processingstatusapi.models.ReportCounts
import gov.cdc.ocio.processingstatusapi.models.reports.*

class ReportCountsQueryService : Query {

    @GraphQLDescription("Returns detailed counts within each stage for the provided uploadId")
    @Suppress("unused")
    fun reportCountsWithUploadId(
        @GraphQLDescription("Upload ID")
        uploadId: String
    ): ReportCounts? = ReportCountsLoader().withUploadId(uploadId)

    @GraphQLDescription("Returns detailed counts within each stage for each matching upload with the provided parameters")
    @Suppress("unused")
    fun reportCountsWithParams(
        @GraphQLDescription("Data stream ID")
        dataStreamId: String,
        @GraphQLDescription("Data stream route")
        dataStreamRoute: String,
        @GraphQLDescription("Start date of the included data.  dateStart or daysInterval is required.")
        dateStart: String? = null,
        @GraphQLDescription("End date of the search.  If not specified then all data up to now is included.")
        dateEnd: String? = null,
        @GraphQLDescription("Number of days to include in the search before today.  If 0, then search for today only.")
        daysInterval: Int? = null,
        pageSize: Int?,
        pageNumber: Int?
    ): AggregateReportCounts = ReportCountsLoader()
        .withParams(
            dataStreamId,
            dataStreamRoute,
            dateStart,
            dateEnd,
            daysInterval,
            pageSize ?: ReportCountsLoader.DEFAULT_PAGE_SIZE,
            pageNumber ?: 1
        )

    @GraphQLDescription("Returns processing counts for the provided parameters")
    @Suppress("unused")
    fun processingCounts(
        @GraphQLDescription("Data stream ID")
        dataStreamId: String,
        @GraphQLDescription("Data stream route")
        dataStreamRoute: String,
        @GraphQLDescription("Start date of the included data.  dateStart or daysInterval is required.")
        dateStart: String? = null,
        @GraphQLDescription("End date of the search.  If not specified then all data up to now is included.")
        dateEnd: String? = null,
        @GraphQLDescription("Number of days to include in the search before today.  If 0, then search for today only.")
        daysInterval: Int? = null
    ): ProcessingCounts = ReportCountsLoader()
        .getProcessingCounts(
            dataStreamId,
            dataStreamRoute,
            dateStart,
            dateEnd,
            daysInterval
        )

    @GraphQLDescription("Returns HL7v2 invalid structure counts for the provided parameters")
    @Suppress("unused")
    fun hl7InvalidStructureValidationCounts(
        @GraphQLDescription("Data stream ID")
        dataStreamId: String,
        @GraphQLDescription("Data stream route")
        dataStreamRoute: String,
        @GraphQLDescription("Start date of the included data.  dateStart or daysInterval is required.")
        dateStart: String? = null,
        @GraphQLDescription("End date of the search.  If not specified then all data up to now is included.")
        dateEnd: String? = null,
        @GraphQLDescription("Number of days to include in the search before today.  If 0, then search for today only.")
        daysInterval: Int? = null
    ): HL7InvalidStructureValidationCounts = ReportCountsLoader()
        .getHL7InvalidStructureValidationCounts(
            dataStreamId,
            dataStreamRoute,
            dateStart,
            dateEnd,
            daysInterval
        )

    @GraphQLDescription("Returns HL7v2 message counts using both a direct and an indirect counting method for the provided parameters")
    @Suppress("unused")
    fun hl7DirectIndirectMessageCounts(
        @GraphQLDescription("Data stream ID")
        dataStreamId: String,
        @GraphQLDescription("Data stream route")
        dataStreamRoute: String,
        @GraphQLDescription("Start date of the included data.  dateStart or daysInterval is required.")
        dateStart: String? = null,
        @GraphQLDescription("End date of the search.  If not specified then all data up to now is included.")
        dateEnd: String? = null,
        @GraphQLDescription("Number of days to include in the search before today.  If 0, then search for today only.")
        daysInterval: Int? = null
    ): HL7DirectIndirectMessageCounts = ReportCountsLoader()
        .getHL7DirectIndirectMessageCounts(
            dataStreamId,
            dataStreamRoute,
            dateStart,
            dateEnd,
            daysInterval
        )

    @GraphQLDescription("Returns HL7v2 invalid message counts using both a direct and an indirect counting method for the provided parameters")
    @Suppress("unused")
    fun hl7InvalidMessageCounts(
        @GraphQLDescription("Data stream ID")
        dataStreamId: String,
        @GraphQLDescription("Data stream route")
        dataStreamRoute: String,
        @GraphQLDescription("Start date of the included data.  dateStart or daysInterval is required.")
        dateStart: String? = null,
        @GraphQLDescription("End date of the search.  If not specified then all data up to now is included.")
        dateEnd: String? = null,
        @GraphQLDescription("Number of days to include in the search before today.  If 0, then search for today only.")
        daysInterval: Int? = null
    ): HL7InvalidMessageCounts = ReportCountsLoader()
        .getHL7InvalidMessageCounts(
            dataStreamId,
            dataStreamRoute,
            dateStart,
            dateEnd,
            daysInterval
        )

    @GraphQLDescription("Returns rolled up counts by stage for the provided parameters")
    @Suppress("unused")
    fun rollupCountsByStage(
        @GraphQLDescription("Data stream ID")
        dataStreamId: String,
        @GraphQLDescription("Data stream route")
        dataStreamRoute: String,
        @GraphQLDescription("Start date of the included data.  dateStart or daysInterval is required.")
        dateStart: String? = null,
        @GraphQLDescription("End date of the search.  If not specified then all data up to now is included.")
        dateEnd: String? = null,
        @GraphQLDescription("Number of days to include in the search before today.  If 0, then search for today only.")
        daysInterval: Int? = null
    ): List<StageCounts> = ReportCountsLoader()
        .rollupCountsByStage(
            dataStreamId,
            dataStreamRoute,
            dateStart,
            dateEnd,
            daysInterval
        )

}
