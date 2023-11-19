package gov.cdc.ocio.processingstatusapi.model

class TraceResult {
    var traceId: String? = null
    var spanId: String? = null
    var upload_id: String? = null
    var timestamp: String? = null
    var status: String? = null
    var elapsed: Int? = null
    var destination_id: String? = null
    var event_type: String? = null
    var metadata : List<Tags>? = null
}