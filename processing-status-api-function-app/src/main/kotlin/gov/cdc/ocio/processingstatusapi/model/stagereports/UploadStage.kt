package gov.cdc.ocio.processingstatusapi.model.stagereports

import java.text.SimpleDateFormat
import java.util.*

/**
 * DEX upload report stage definition.
 *
 * @property tguid String?
 * @property offset Long
 * @property size Long
 * @property filename String?
 * @property metadata Map<String, Any>?
 * @property start_time_epoch_millis Long
 * @property end_time_epoch_millis Long
 */
class UploadStage: SchemaDefinition() {

    var tguid : String? = null

    var offset : Long = 0

    var size : Long = 0

    var filename : String? = null

    var metadata : Map<String, Any>? = null

    var start_time_epoch_millis: Long = 0

    var end_time_epoch_millis: Long = 0

    fun getTimestamp(): String {
        val date = Date(start_time_epoch_millis)
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(date)
    }

    companion object {
        val schemaDefinition = SchemaDefinition(schemaName = "upload", schemaVersion = "1.0")
    }
}