package gov.cdc.ocio.processingstatusapi.models.dao

import gov.cdc.ocio.processingstatusapi.models.query.DuplicateFilenameCounts


/**
 * Data access object for duplicate filename counts.
 *
 * @property filename String?
 * @property totalCount Long
 * @constructor
 */
data class DuplicateFilenameCountsDao(

    var filename: String? = null,

    var totalCount: Long = 0
) {

    /**
     * Convert the data access object into a graphql one.
     *
     * @return DuplicateFilenameCounts
     */
    fun toDuplicateFilenameCounts() = DuplicateFilenameCounts().apply {
        this.filename = this@DuplicateFilenameCountsDao.filename
        this.totalCount = this@DuplicateFilenameCountsDao.totalCount
    }
}