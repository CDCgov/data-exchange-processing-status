package gov.cdc.ocio.processingstatusapi.model.reports.stagereports

import com.google.gson.annotations.SerializedName
import gov.cdc.ocio.model.message.SchemaDefinition
import java.util.*

/**
 * DEX upload report stage definition.
 *
 * @property tguid String?
 * @property offset Long
 * @property size Long
 * @property filename String?
 * @property metadata Map<String, Any>?
 * @property startTimeEpochMillis Long
 * @property endTimeEpochMillis Long
 */
class UploadStage: SchemaDefinition() {

    var tguid : String? = null

    var offset : Long = 0

    var size : Long = 0

    var filename : String? = null

    var metadata : Map<String, Any>? = null

    @SerializedName("start_time_epoch_millis")
    var startTimeEpochMillis: Long = 0

    @SerializedName("end_time_epoch_millis")
    var endTimeEpochMillis: Long = 0

    fun getTimestamp(): Date {
        return Date(startTimeEpochMillis)
    }

    companion object {
        val schemaDefinition = SchemaDefinition(schemaName = "upload", schemaVersion = "1.0")
    }
}