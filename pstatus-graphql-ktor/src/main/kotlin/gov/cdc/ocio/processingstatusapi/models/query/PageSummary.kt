package gov.cdc.ocio.processingstatusapi.models.query

/**
 * Page summary data class definition.
 *
 * @property pageNumber Int
 * @property numberOfPages Int
 * @property pageSize Int
 * @property totalItems Int
 * @constructor
 */
data class PageSummary(

    var pageNumber: Int = 0,

    var numberOfPages: Int = 0,

    var pageSize: Int = 0,

    var totalItems: Int = 0
)