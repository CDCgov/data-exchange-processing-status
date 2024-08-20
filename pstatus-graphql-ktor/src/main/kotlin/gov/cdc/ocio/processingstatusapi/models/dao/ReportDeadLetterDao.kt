package gov.cdc.ocio.processingstatusapi.models.dao

import gov.cdc.ocio.processingstatusapi.models.ReportDeadLetter
import java.time.ZoneOffset


/**
 * Data access object for dead-letter reports, which is the structure returned from CosmosDB queries.
 */
data class ReportDeadLetterDao(

    var dispositionType: String? = null,

    var deadLetterReasons: List<String>? = null,

    var validationSchemas: List<String>? = null,

) : ReportDao() {
    /**
     * Convenience function to convert a cosmos data object to a ReportDeadLetter object
     */
    fun toReportDeadLetter() = ReportDeadLetter().apply {
        this.id = this@ReportDeadLetterDao.id
        this.uploadId = this@ReportDeadLetterDao.uploadId
        this.reportId = this@ReportDeadLetterDao.reportId
        this.dataStreamId = this@ReportDeadLetterDao.dataStreamId
        this.dataStreamRoute = this@ReportDeadLetterDao.dataStreamRoute
        this.dexIngestDateTime = this@ReportDeadLetterDao.dexIngestDateTime?.toInstant()?.atOffset(ZoneOffset.UTC)
        this.messageMetadata = this@ReportDeadLetterDao.messageMetadata?.toMessageMetadata()
        this.stageInfo = this@ReportDeadLetterDao.stageInfo?.toStageInfo()
        this.tags = this@ReportDeadLetterDao.tags
        this.data = this@ReportDeadLetterDao.data
        this.jurisdiction = this@ReportDeadLetterDao.jurisdiction
        this.senderId = this@ReportDeadLetterDao.senderId
        this.timestamp = this@ReportDeadLetterDao.timestamp?.toInstant()?.atOffset(ZoneOffset.UTC)
        this.contentType = this@ReportDeadLetterDao.contentType
        this.content = this@ReportDeadLetterDao.content as? Map<*, *>
        this.dispositionType = this@ReportDeadLetterDao.dispositionType
        this.deadLetterReasons = this@ReportDeadLetterDao.deadLetterReasons
        this.validationSchemas = this@ReportDeadLetterDao.validationSchemas
        this.jurisdiction = this@ReportDeadLetterDao.jurisdiction
        this.senderId = this@ReportDeadLetterDao.senderId
    }

}