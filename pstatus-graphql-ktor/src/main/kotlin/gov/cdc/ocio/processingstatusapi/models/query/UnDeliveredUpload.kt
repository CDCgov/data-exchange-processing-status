package gov.cdc.ocio.processingstatusapi.models.query

import com.expediagroup.graphql.generator.annotations.GraphQLDescription

@GraphQLDescription("Collection of undelivered found")
data class UnDeliveredUpload(

    @GraphQLDescription("UploadId of the file that is not delivered.")
    var uploadId: String? = null,

    @GraphQLDescription("Filename of the file that is not delivered.")
    var filename: String? = null
)