package gov.cdc.ocio.processingstatusapi.models.reports

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
class UploadStatusContent: SchemaDefinition {

    override val schemaName = "upload"

    override val schemaVersion = "1.0.0"

    var tguid : String? = null

    var offset : Int? = 0

    var size : Int? = 0

    var filename : String? = null

//    var metadata : Map<String, Any>? = null

    var startTimeEpochMillis: Int? = 0

    var endTimeEpochMillis: Int? = 0

//    fun getTimestamp(): Date {
//        return Date(startTimeEpochMillis?.toLong() ?: 0)
//    }
}