package gov.cdc.ocio.processingstatusapi.models.dao

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import gov.cdc.ocio.processingstatusapi.models.reports.BaseContent
import gov.cdc.ocio.processingstatusapi.models.reports.MetadataVerifyContent
import gov.cdc.ocio.processingstatusapi.models.reports.UploadStatusContent
import java.util.*

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

    var timestamp: Float? = null, // TODO: Date

    var content: Any? = null
) {
    val contentAsType: BaseContent?
        get() {
            if (content == null) return null

            return when (contentType?.lowercase(Locale.getDefault())) {
                "json" -> {
                    if (content is LinkedHashMap<*, *>) {
                        val json = Gson().toJson(content, MutableMap::class.java).toString()
                        when ((content as LinkedHashMap<*, *>)["schema_name"]) {
                            "dex-metadata-verify" -> return Gson().fromJson(json, MetadataVerifyContent::class.java)
                            "upload" -> return Gson().fromJson(json, UploadStatusContent::class.java)
                            else -> return null
                        }
                    } else
                        null//content.toString()
                }

                else -> null//content.toString()
            }
        }
}