package gov.cdc.ocio.processingstatusapi.models.reports

import com.expediagroup.graphql.generator.annotations.GraphQLDescription

/**
 * DEX upload report stage definition.
 *
 * @property tguid String?
 * @property offset Long
 * @property size Long
 * @property filename String?
 * @property metadata Map<String, Any>?
 * @property startTimeEpochMillis Long
 * @property endTimeEpochMillis Long
 */
@GraphQLDescription("Upload status report content")
class UploadStatusContent: SchemaDefinition {

    @GraphQLDescription("Schema name for the report content")
    override val schemaName = "upload"

    @GraphQLDescription("Version of the schema for the report content")
    override val schemaVersion = "1.0.0"

    @GraphQLDescription("Transfer GUID assigned by the Upload API when the upload begins")
    var tguid : String? = null

    @GraphQLDescription("Current offset in bytes for the upload.  If the upload is in progress the offset will be less than the size field.  If the upload is complete then offset will equal size.")
    var offset : Long? = 0

    @GraphQLDescription("Expected size of the file uploaded in bytes")
    var size : Long? = 0

    @GraphQLDescription("Filename of the file being uploaded")
    var filename : String? = null

    @GraphQLDescription("Metadata associated with the file being uploaded")
    var metadata : Map<String, Any>? = null

    @GraphQLDescription("Epoch time in milliseconds of when the upload started")
    var startTimeEpochMillis: Int? = 0

    @GraphQLDescription("Epoch time in milliseconds of when the upload finished")
    var endTimeEpochMillis: Int? = 0
}