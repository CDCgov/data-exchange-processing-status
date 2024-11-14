package gov.cdc.ocio.processingstatusapi.models.query

import com.expediagroup.graphql.generator.annotations.GraphQLDescription

@GraphQLDescription("Collection of undelivered uploads found")
data class UndeliveredUploadCounts(

    @GraphQLDescription("Total number of undelivered uploads.")
    var totalCount: Long = 0,

    @GraphQLDescription("Provides a list of all the uploads that have not been delivered. This means, the upload started, but according to the upload status reports we did not receive 100% of the expected chunks.")
    var undeliveredUploads: List<UndeliveredUpload> = listOf()
)