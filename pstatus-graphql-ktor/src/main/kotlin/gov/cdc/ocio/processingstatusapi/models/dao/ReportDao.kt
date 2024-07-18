package gov.cdc.ocio.processingstatusapi.models.dao

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import gov.cdc.ocio.processingstatusapi.models.Report
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
 * @property stageName String?
 * @property contentType String?
 * @property messageId String?
 * @property status String?
 * @property timestamp Date?
 * @property content Any?
 * @property contentAsString String?
 * @constructor
 */
data class ReportDao(

    var id : String? = null,

    @SerializedName("upload_id")
    var uploadId: String? = null,

    @SerializedName("report_id")
    var reportId: String? = null,

    @SerializedName("data_stream_id")
    var dataStreamId: String? = null,

    @SerializedName("data_stream_route")
    var dataStreamRoute: String? = null,

    @SerializedName("stage_name")
    var stageName: String? = null,

    @SerializedName("content_type")
    var contentType : String? = null,

    @SerializedName("message_id")
    var messageId: String? = null,

    @SerializedName("status")
    var status : String? = null,

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
        this.messageId = this@ReportDao.messageId
        this.status = this@ReportDao.status
        this.timestamp = this@ReportDao.timestamp?.toInstant()?.atOffset(ZoneOffset.UTC)
        this.contentType = this@ReportDao.contentType
        this.content = this@ReportDao.content as? Map<*, *>
    }
}