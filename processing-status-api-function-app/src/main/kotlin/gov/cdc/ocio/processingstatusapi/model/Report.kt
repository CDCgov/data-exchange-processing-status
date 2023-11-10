package gov.cdc.ocio.processingstatusapi.model

import java.util.*

class Report {

    var id : String? = null

    var reportId: String? = null

    var uploadId: String? = null

    var destinationId: String? = null

    var reports: List<StageReport>? = null
}

class StageReport {

    var reportId: String? = null

    var stageName: String? = null

    var contentType : String? = null

    var content: String? = null

    val timestamp: Date = Date()
}