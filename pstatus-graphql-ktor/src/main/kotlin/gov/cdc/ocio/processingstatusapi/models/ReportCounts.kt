package gov.cdc.ocio.processingstatusapi.models

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import java.time.OffsetDateTime

@GraphQLDescription("Report counts for a given upload")
data class ReportCounts(

    @GraphQLDescription("Upload ID of the report counts")
    var uploadId: String? = null,

    @GraphQLDescription("Data stream ID")
    var dataStreamId: String? = null,

    @GraphQLDescription("Data stream route")
    var dataStreamRoute: String? = null,

    @GraphQLDescription("Earliest timestamp associated with this upload")
    var timestamp: OffsetDateTime? = null,

    @GraphQLDescription("Processing stages this upload went through")
    var stages: Map<String, Any> = mapOf()
)
