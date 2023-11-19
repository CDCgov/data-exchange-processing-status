package gov.cdc.ocio.processingstatusapi.model

class Spans {
    var traceID : String = ""
    var spanID : String = ""
    var operationName : String = ""
    var references : List<String>? = null
    var startTime : Long = 0
    var duration : Long = 0
    var tags : List<Tags>? = null
    var logs : List<String>? = null
    var processID : String = ""
    var warnings : String = ""
}