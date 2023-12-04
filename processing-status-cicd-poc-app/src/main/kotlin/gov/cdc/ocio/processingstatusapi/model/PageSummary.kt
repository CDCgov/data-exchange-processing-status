package gov.cdc.ocio.processingstatusapi.model

import com.google.gson.annotations.SerializedName

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

    @SerializedName("page_number")
    var pageNumber: Int = 0,

    @SerializedName("number_of_pages")
    var numberOfPages: Int = 0,

    @SerializedName("page_size")
    var pageSize: Int = 0,

    @SerializedName("total_items")
    var totalItems: Long = 0
)