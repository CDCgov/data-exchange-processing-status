package gov.cdc.ocio.processingstatusapi.models.query

import com.expediagroup.graphql.generator.annotations.GraphQLDescription

/**
 * Page summary data class definition.
 *
 * @property pageNumber Int
 * @property numberOfPages Int
 * @property pageSize Int
 * @property totalItems Int
 * @constructor
 */
@GraphQLDescription("Page summary for a response to a query")
data class PageSummary(

    @GraphQLDescription("Page number provided in the response")
    var pageNumber: Int = 0,

    @GraphQLDescription("Total number of pages for the page size given")
    var numberOfPages: Int = 0,

    @GraphQLDescription("Page size of the items in the response matching the search criteria")
    var pageSize: Int = 0,

    @GraphQLDescription("Total number of items that can be provided matching the search criteria")
    var totalItems: Int = 0,

    @GraphQLDescription("List of all the senderIds in the entire dataset matching the search criteria, not just this page.")
    var senderIds: List<String> = listOf(),

    @GraphQLDescription("List of all the jurisdictions in the entire dataset matching the search criteria, not just this page.")
    var jurisdictions: List<String> = listOf()
)