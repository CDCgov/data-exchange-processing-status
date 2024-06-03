package gov.cdc.ocio.processingstatusapi.models.query

import com.expediagroup.graphql.generator.annotations.GraphQLDescription

/**
 * Upload status response definition.
 *
 * @property summary PageSummary
 * @property items MutableList<UploadStatus>
 */

@GraphQLDescription("Upload statuses for the provided parameters")
data class UploadsStatus(

    @GraphQLDescription("Page summary for the upload statuses provided")
    var summary: PageSummary = PageSummary(),

    @GraphQLDescription("Upload status items")
    var items: MutableList<UploadStatus> = mutableListOf()
)