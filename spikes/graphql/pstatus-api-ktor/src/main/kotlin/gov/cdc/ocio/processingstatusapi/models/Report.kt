package gov.cdc.ocio.processingstatusapi.models

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.google.gson.*
import com.google.gson.annotations.SerializedName
import java.util.*

interface BaseContent {
    var schema_name: String

    var schema_version: String
}

class MetadataVerifyContent : BaseContent {
    override lateinit var schema_name: String

    override lateinit var schema_version: String

    var filename: String? = null
}

class UploadStatusContent: BaseContent {
    override lateinit var schema_name: String

    override lateinit var schema_version: String

    var offset: Int? = null

    var size: Int? = null
}

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

/**
 * Report for a given stage.
 *
 * @property uploadId String?
 * @property reportId String?
 * @property dataStreamId String?
 * @property dataStreamRoute String?
 * @property stageName String?
 * @property contentType String?
 * @property messageId String?
 * @property status String?
 * @property content String?
 * @property timestamp Date
 */
@GraphQLDescription("Contains Report content.")
data class Report(

    var id : String? = null,

    var uploadId: String? = null,

    var reportId: String? = null,

    var dataStreamId: String? = null,

    var dataStreamRoute: String? = null,

    var stageName: String? = null,

    var contentType : String? = null,

    var messageId: String? = null,

    var status : String? = null,

    var content: BaseContent? = null,

    var timestamp: Float? = null // TODO: Date
)