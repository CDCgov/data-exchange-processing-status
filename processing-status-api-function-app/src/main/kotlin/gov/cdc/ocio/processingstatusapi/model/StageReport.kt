package gov.cdc.ocio.processingstatusapi.model

import java.util.*

class StageReport {

    var reportId: String? = null

    var stageName: String? = null

    var contentType : String? = null

    var content: String? = null

    val timestamp: Date = Date()
}