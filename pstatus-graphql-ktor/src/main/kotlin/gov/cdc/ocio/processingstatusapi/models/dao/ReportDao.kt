package gov.cdc.ocio.processingstatusapi.models.dao

import com.google.gson.Gson
import gov.cdc.ocio.processingstatusapi.models.Report
import gov.cdc.ocio.processingstatusapi.models.submission.MessageMetadata
import java.time.ZoneOffset
import java.util.*

/**
 * Data access object for reports, which is the structure returned from CosmosDB queries.
 *
 * @property id String?
 * @property uploadId String?
 * @property reportId String?
 * @property dataStreamId String?
 * @property dataStreamRoute String?
 * @property dexIngestDateTime Date?
 * @property messageMetadata MessageMetadata?
 * @property stageInfo StageInfo?
 * @property tags Map<String,String??
 * @property data Map<String,String>?
 * @property contentType String?
 * @property messageId String?
 * @property timestamp Date?
 * @property content Any?
 * @property jurisdiction String?
 * @property senderId String?
 * @property contentAsString String?
 * @constructor
 */
open class ReportDao(

    var id : String? = null,

    var uploadId: String? = null,

    var reportId: String? = null,

    var dataStreamId: String? = null,

    var dataStreamRoute: String? = null,

    var dexIngestDateTime: Date? = null,

    var messageMetadata: MessageMetadata? = null,

    var stageInfo: StageInfoDao? = null,

    var tags: Map<String, String>? = null,

    var data: Map<String, String>? = null,

    var contentType : String? = null,

    var jurisdiction:String? =null,

    var senderId:String? = null,

    var messageId: String? = null,

    var timestamp: Date? = null,

    var content: Any? = null
) {

    val contentAsString: String?
        get() {
            if (content == null) return null

            return when (contentType?.lowercase(Locale.getDefault())) {
                "json" -> {
                    if (content is LinkedHashMap<*, *>)
                        Gson().toJson(content, MutableMap::class.java).toString()
                    else
                        content.toString()
                }
                else -> content.toString()
            }
        }

    /**
     * Convenience function to convert a cosmos data object to a Report object
     */
    fun toReport() = Report().apply {
        this.id = this@ReportDao.id
        this.uploadId = this@ReportDao.uploadId
        this.reportId = this@ReportDao.reportId
        this.dataStreamId = this@ReportDao.dataStreamId
        this.dataStreamRoute = this@ReportDao.dataStreamRoute
        this.dexIngestDateTime = this@ReportDao.dexIngestDateTime?.toInstant()?.atOffset(ZoneOffset.UTC)
        this.messageMetadata = this@ReportDao.messageMetadata
        this.stageInfo = this@ReportDao.stageInfo?.toStageInfo()
        this.tags = this@ReportDao.tags
        this.data = this@ReportDao.data
        this.messageId = this@ReportDao.messageId
        this.jurisdiction = this@ReportDao.jurisdiction
        this.senderId = this@ReportDao.senderId
        this.timestamp = this@ReportDao.timestamp?.toInstant()?.atOffset(ZoneOffset.UTC)
        this.contentType = this@ReportDao.contentType
        this.content = this@ReportDao.content as? Map<*, *>
    }
}