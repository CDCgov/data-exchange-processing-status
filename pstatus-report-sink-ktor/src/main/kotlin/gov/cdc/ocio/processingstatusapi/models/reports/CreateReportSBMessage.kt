package gov.cdc.ocio.processingstatusapi.models.reports

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import gov.cdc.ocio.processingstatusapi.exceptions.BadStateException
import gov.cdc.ocio.processingstatusapi.models.ServiceBusMessage
import java.lang.ClassCastException
import java.util.*

/**
 * Create a report service bus message.
 *
 * @property uploadId String?
 * @property dataStreamId String?
 * @property dataStreamRoute String?
 * @property stageName String?
 * @property contentType String?
 * @property content String?
 */
class CreateReportSBMessage: ServiceBusMessage() {

    @SerializedName("upload_id")
    val uploadId: String? = null

    @SerializedName("data_stream_id")
    val dataStreamId: String? = null

    @SerializedName("data_stream_route")
    val dataStreamRoute: String? = null

    @SerializedName("stage_name")
    val stageName: String? = null

    @SerializedName("content_type")
    val contentType: String? = null

    @SerializedName("message_id")
    var messageId: String? = null

    @SerializedName("status")
    var status : String? = null

    // content will vary depending on content_type so make it any.  For example, if content_type is json then the
    // content type will be a Map<*, *>.
    val content: Any? = null


}