package gov.cdc.ocio.processingstatusapi.model

class Base {
    var data : List<Data>? = null
    var total : Int = 0
    var limit : Int = 0
    var offset : Int = 0
    var errors : String = ""
}