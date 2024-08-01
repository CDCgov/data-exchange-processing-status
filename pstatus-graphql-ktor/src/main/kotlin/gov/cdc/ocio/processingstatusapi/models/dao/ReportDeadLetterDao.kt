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
        this.messageId = this@ReportDeadLetterDao.messageId
        this.status = this@ReportDeadLetterDao.status
        this.timestamp = this@ReportDeadLetterDao.timestamp?.toInstant()?.atOffset(ZoneOffset.UTC)
        this.contentType = this@ReportDeadLetterDao.contentType
        this.content = this@ReportDeadLetterDao.content as? Map<*, *>
        this.dispositionType = this@ReportDeadLetterDao.dispositionType
        this.deadLetterReasons = this@ReportDeadLetterDao.deadLetterReasons
        this.validationSchemas = this@ReportDeadLetterDao.validationSchemas
    }

}