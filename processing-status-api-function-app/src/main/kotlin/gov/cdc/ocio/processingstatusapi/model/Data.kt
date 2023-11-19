package gov.cdc.ocio.processingstatusapi.model

class Data {
    var traceID : String = ""
    var spans : List<Spans>? = null
    var processes : Processes? = null
    var warnings : String = ""
}