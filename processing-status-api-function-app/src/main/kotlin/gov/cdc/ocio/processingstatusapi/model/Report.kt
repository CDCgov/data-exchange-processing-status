package gov.cdc.ocio.processingstatusapi.model

class Report {

    var id : String? = null

    var reportId: String? = null

    var uploadId: String? = null

    var destinationId: String? = null

    var eventType: String? = null

    var reports: List<StageReport>? = null
}
