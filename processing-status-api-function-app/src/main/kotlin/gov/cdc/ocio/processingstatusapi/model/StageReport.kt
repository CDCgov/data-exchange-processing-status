package gov.cdc.ocio.processingstatusapi.model

import java.util.*

/**
 * Report for a given stage.
 *
 * @property reportId String?
 * @property stageName String?
 * @property contentType String?
 * @property content String?
 * @property timestamp Date
 */
class StageReport {

    var reportId: String? = null

    var stageName: String? = null

    var contentType : String? = null

    var content: String? = null

    val timestamp: Date = Date()
}