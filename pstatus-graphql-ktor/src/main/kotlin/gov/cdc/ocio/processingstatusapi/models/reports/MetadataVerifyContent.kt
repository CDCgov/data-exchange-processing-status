package gov.cdc.ocio.processingstatusapi.models.reports

import com.expediagroup.graphql.generator.annotations.GraphQLDescription

@GraphQLDescription("Metadata verify report content")
class MetadataVerifyContent : SchemaDefinition {

    @GraphQLDescription("Schema name for the report content")
    override val schemaName: String = "upload-status"

    @GraphQLDescription("Version of the schema for the report content")
    override val schemaVersion: String = "1.0.0"

    @GraphQLDescription("Filename of the uploaded file")
    var filename: String? = null

    @GraphQLDescription("Metadata associated with the uploaded file")
    var metadata : Map<String, Any>? = null

    @GraphQLDescription("Issues found with the metadata during validation.  The issues will be an empty array or null if no issues were found.")
    var issues: List<String>? = null
}