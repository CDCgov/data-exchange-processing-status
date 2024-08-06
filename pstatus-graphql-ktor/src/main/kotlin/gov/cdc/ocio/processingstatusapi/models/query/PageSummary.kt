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

    @GraphQLDescription("Page size of the response")
    var pageSize: Int = 0,

    @GraphQLDescription("Total number of items that can be provided")
    var totalItems: Int = 0,

    @GraphQLDescription("List of the senderIds")
    var senderIds: List<String> = listOf(),

    @GraphQLDescription("List of the jurisdictions")
    var jurisdictions: List<String> = listOf()
)