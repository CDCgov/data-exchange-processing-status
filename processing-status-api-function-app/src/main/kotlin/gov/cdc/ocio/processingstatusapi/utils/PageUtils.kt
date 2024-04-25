package gov.cdc.ocio.processingstatusapi.utils

import gov.cdc.ocio.processingstatusapi.exceptions.BadRequestException

class PageUtils(private val minPageSize: Int,
                private val maxPageSize: Int,
                private val defaultPageSize: Int) {

    private constructor(builder: Builder) : this(builder.minPageSize, builder.maxPageSize, builder.defaultPageSize)

    class Builder {
        var minPageSize: Int = 1
            private set

        var maxPageSize: Int = 100
            private set

        var defaultPageSize: Int = 20
            private set

        fun setMinPageSize(minPageSize: Int) = apply { this.minPageSize = minPageSize }

        fun setMaxPageSize(maxPageSize: Int) = apply { this.maxPageSize = maxPageSize }

        fun setDefaultPageSize(defaultPageSize: Int) = apply { this.defaultPageSize = defaultPageSize }

        fun build() = PageUtils(this)
    }

    /**
     * Get page size if valid.
     *
     * @param pageSize String?
     * @return Int
     * @throws BadRequestException
     */
    @Throws(BadRequestException::class)
    fun getPageSize(pageSize: String?) = run {
        var pageSizeAsInt = defaultPageSize
        pageSize?.run {
            var issue = false
            try {
                pageSizeAsInt = pageSize.toInt()
                if (pageSizeAsInt < minPageSize || pageSizeAsInt > maxPageSize)
                    issue = true
            } catch (e: NumberFormatException) {
                issue = true
            }

            if (issue) {
                throw BadRequestException("page_size must be between $minPageSize and $maxPageSize")
            }
        }
        pageSizeAsInt
    }

    companion object {

        /**
         * Get page number if valid.
         *
         * @param pageNumber String?
         * @param numberOfPages Int
         * @return Int
         * @throws BadRequestException
         */
        @Throws(BadRequestException::class)
        fun getPageNumber(pageNumber: String?, numberOfPages: Int) = run {
            var pageNumberAsInt = 1
            pageNumber?.run {
                var issue = false
                try {
                    pageNumberAsInt = pageNumber.toInt()
                    if (pageNumberAsInt < 1 || pageNumberAsInt > numberOfPages)
                        issue = true
                } catch (e: NumberFormatException) {
                    issue = true
                }

                if (issue) {
                    throw BadRequestException("page_number must be between 1 and $numberOfPages")
                }
            }
            pageNumberAsInt
        }
    }
}