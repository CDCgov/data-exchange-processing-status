package gov.cdc.ocio.processingstatusapi.models.schemas

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gov.cdc.ocio.reportschemavalidator.models.ReportSchemaMetadata


@GraphQLDescription("Describes a report schema.")
data class ReportSchemaMetadataGql(

    @GraphQLDescription("Filename of the report schema.")
    val filename: String,

    @GraphQLDescription("Name of the report schema.")
    val schemaName: String,

    @GraphQLDescription("Version of the report schema.")
    val schemaVersion: String,

    @GraphQLDescription("Description of the report schema.")
    val description: String
) {
    companion object {
        fun from(reportSchemaMetadata: ReportSchemaMetadata) = ReportSchemaMetadataGql(
            reportSchemaMetadata.filename,
            reportSchemaMetadata.schemaName,
            reportSchemaMetadata.schemaVersion,
            reportSchemaMetadata.description
        )
    }
}