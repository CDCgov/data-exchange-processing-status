package gov.cdc.ocio.processingstatusapi.models.schemas

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gov.cdc.ocio.reportschemavalidator.models.SchemaLoaderInfo


@GraphQLDescription("Provides information about the schema loader being used for report validations.")
data class SchemaLoaderInfoGql(

    @GraphQLDescription("Schema loader system being utilized.")
    val loaderType: String,

    @GraphQLDescription("Location of the schema files.")
    val location: String
) {
    companion object {
        fun from(schemaLoaderInfo: SchemaLoaderInfo) = SchemaLoaderInfoGql(
            schemaLoaderInfo.type,
            schemaLoaderInfo.location
        )
    }
}