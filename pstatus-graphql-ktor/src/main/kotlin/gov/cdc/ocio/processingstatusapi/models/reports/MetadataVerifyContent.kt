package gov.cdc.ocio.processingstatusapi.models.reports

class MetadataVerifyContent : SchemaDefinition {

    override val schemaName: String = "dex-metadata-verify"

    override val schemaVersion: String = "0.0.1"

    var filename: String? = null

//    var metadata : Map<String, Any>? = null
//
//    var issues: List<String>? = null
}