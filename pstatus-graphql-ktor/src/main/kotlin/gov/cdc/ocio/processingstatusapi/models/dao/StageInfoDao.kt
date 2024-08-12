package gov.cdc.ocio.processingstatusapi.models.dao

import gov.cdc.ocio.processingstatusapi.models.submission.Issue
import gov.cdc.ocio.processingstatusapi.models.submission.StageInfo
import gov.cdc.ocio.processingstatusapi.models.submission.Status
import java.time.ZoneOffset
import java.util.Date

/**
 * Data access object for report stage info, which is the structure returned from CosmosDB queries.
 *
 * @property service String?
 * @property action String?
 * @property version String?
 * @property status Status?
 * @property issues List<Issue>?
 * @property startProcessingTime Date?
 * @property endProcessingTime Date?
 * @constructor
 */
data class StageInfoDao(

    var service : String? = null,

    var action: String? = null,

    var version: String? = null,

    var status: Status? = null,

    var issues: List<Issue>? = null,

    var startProcessingTime: Date? = null,

    var endProcessingTime: Date? = null
) {
    /**
     * Convenience function to convert a cosmos data object to a StageInfo object
     */
    fun toStageInfo(): StageInfo {
        return StageInfo().apply {
            this.service = this@StageInfoDao.service
            this.action = this@StageInfoDao.action
            this.version = this@StageInfoDao.version
            this.status = this@StageInfoDao.status
            this.issues = this@StageInfoDao.issues
            this.startProcessingTime = this@StageInfoDao.startProcessingTime?.toInstant()?.atOffset(ZoneOffset.UTC)
            this.endProcessingTime = this@StageInfoDao.endProcessingTime?.toInstant()?.atOffset(ZoneOffset.UTC)
        }
    }
}