package gov.cdc.ocio.processingstatusapi.models.reports.stagereports

/**
 * DEX upload metadata verify stage definition.
 *
 * @property filename String?
 * @property metadata Map<String, Any>?
 * @property issues List<String>?
 */
class UploadMetadataVerifyStage: SchemaDefinition(priority = -1) {

    var filename : String? = null

    var metadata : Map<String, Any>? = null

    var issues: List<String>? = null

    companion object {
        val schemaDefinition = SchemaDefinition(schemaName = "upload-status", schemaVersion = "1.0.0")
    }
}