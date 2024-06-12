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
    fun getPageSize(pageSize: Int?) = run {
        pageSize?.run {
            var issue = false
            try {
                if (pageSize < minPageSize || pageSize > maxPageSize)
                    issue = true
            } catch (e: NumberFormatException) {
                issue = true
            }

            if (issue) {
                throw BadRequestException("pageSize must be between $minPageSize and $maxPageSize")
            }
        }
        pageSize
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
        fun getPageNumber(pageNumber: Int?, numberOfPages: Int) = run {
            var pageNumberResult = 1
            pageNumber?.run {
                var issue = false
                try {
                    pageNumberResult = pageNumber
                    if (pageNumber < 1 || pageNumber > numberOfPages)
                        issue = true
                } catch (e: NumberFormatException) {
                    issue = true
                }

                if (issue) {
                    throw BadRequestException("page_number must be between 1 and $numberOfPages")
                }
            }
            pageNumberResult
        }
    }
}