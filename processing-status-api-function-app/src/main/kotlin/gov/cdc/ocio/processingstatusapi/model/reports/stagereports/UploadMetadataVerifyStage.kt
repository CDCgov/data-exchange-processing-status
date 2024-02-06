package gov.cdc.ocio.processingstatusapi.model.reports.stagereports

/**
 * DEX upload metadata verify stage definition.
 *
 * @property filename String?
 * @property metadata Map<String, Any>?
 * @property issues List<String>?
 */
class UploadMetadataVerifyStage: SchemaDefinition() {

    var filename : String? = null

    var metadata : Map<String, Any>? = null

    var issues: List<String>? = null

    companion object {
        val schemaDefinition = SchemaDefinition(schemaName = "dex-metadata-verify", schemaVersion = "0.0.1")
    }
}