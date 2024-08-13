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

    @GraphQLDescription("Page summary for the upload statuses returned from the search")
    var summary: PageSummary = PageSummary(),

    @GraphQLDescription("A list of all the Upload status items matching the Search Criteria")
    var items: MutableList<UploadStatus> = mutableListOf()
)