package gov.cdc.ocio.processingstatusapi.queries

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query
import gov.cdc.ocio.processingstatusapi.loaders.ReportDeadLetterLoader


class ReportDeadLetterQueryService : Query {

    @GraphQLDescription("Return all the dead-letter reports associated with the provided uploadId")
    @Suppress("unused")
    fun getDeadLetterReportsByUploadId(uploadId: String) = ReportDeadLetterLoader().getByUploadId(uploadId)

    @GraphQLDescription("Return all the dead-letter reports associated with the provided datastreamId, datastreamroute and timestamp date range")
    @Suppress("unused")
    fun getDeadLetterReportsByDataStream(dataStreamId: String, dataStreamRoute:String, startDate:String?, endDate:String?, daysInterval :Int?)
              = ReportDeadLetterLoader().getByDataStreamByDateRange(dataStreamId,dataStreamRoute,startDate,endDate,daysInterval)

    @GraphQLDescription("Return count of dead-letter reports associated with the provided datastreamId, (optional) datastreamroute and timestamp date range")
    @Suppress("unused")
    fun getDeadLetterReportsCountByDataStream(dataStreamId: String, dataStreamRoute:String?, startDate:String?, endDate:String?, daysInterval:Int?)
            = ReportDeadLetterLoader().getCountByDataStreamByDateRange(dataStreamId,dataStreamRoute,startDate,endDate,daysInterval)
}