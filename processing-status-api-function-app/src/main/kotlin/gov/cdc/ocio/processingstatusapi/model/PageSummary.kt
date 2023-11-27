package gov.cdc.ocio.processingstatusapi.model

/**
 * Page summary data class definition.
 *
 * @property pageNumber Int
 * @property numberOfPages Int
 * @property pageSize Int
 * @property totalItems Long
 * @constructor
 */
data class PageSummary(

    var pageNumber: Int = 0,

    var numberOfPages: Int = 0,

    var pageSize: Int = 0,

    var totalItems: Long = 0
)