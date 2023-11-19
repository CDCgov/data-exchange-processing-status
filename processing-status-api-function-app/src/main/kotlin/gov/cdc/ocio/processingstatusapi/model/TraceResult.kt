package gov.cdc.ocio.processingstatusapi.model

class TraceResult {

    var status: String? = null
    var traceId: String? = null
    var spanId: String? = null
    var upload_id: String? = null
    var timestamp: String? = null
    var elapsed: Long? = null
    var destination_id: String? = null
    var event_type: String? = null
    var metadata : List<Tags>? = null

}