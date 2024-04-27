package gov.cdc.ocio.processingstatusapi.models

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.google.gson.*
import com.google.gson.annotations.SerializedName
import java.util.*

data class MetadataVerifyContent(

    var schema_name: String? = null,

    var schema_version: String? = null,

    var filename: String? = null
)

data class UploadStatusContent(

    var schema_name: String? = null,

    var schema_version: String? = null,

    var offset: Int? = null,

    var size: Int?= null
)

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

//    var content: Any? = null,

//    val timestamp: Date = Date()
) {
//    val contentAsString: String?
//        get() {
//            if (content == null) return null
//
//            return when (contentType?.lowercase(Locale.getDefault())) {
//                "json" -> {
//                    if (content is LinkedHashMap<*, *>)
//                        Gson().toJson(content, MutableMap::class.java).toString()
//                    else
//                        content.toString()
//                }
//                else -> content.toString()
//            }
//        }
}

// Defined in some other library
//class SharedModel(val foo: String)

// Our code
//class ServiceModel(val bar: String)

//class Query {
//    @GraphQLUnion(
//        name = "CustomUnion",
//        possibleTypes = [SharedModel::class, ServiceModel::class],
//        description = "Return one or the other model"
//    )
//    fun getModel(): Any = ServiceModel("abc")
//}