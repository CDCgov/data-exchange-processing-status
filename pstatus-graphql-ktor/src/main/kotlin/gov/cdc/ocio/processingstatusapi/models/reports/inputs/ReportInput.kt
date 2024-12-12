package gov.cdc.ocio.processingstatusapi.models.reports.inputs

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import gov.cdc.ocio.processingstatusapi.plugins.CustomHashMap


@GraphQLDescription("Input type for creating or updating a report")
data class ReportInput(

    @GraphQLDescription("Indicates the content type of the content; e.g. JSON, APPLICATION/JSON, XML, BASE64")
    val contentType: String? = null,

    @GraphQLDescription("Content of the report. If the report is JSON then the content will be a map, otherwise, it will be a string")
    var content: CustomHashMap<String, Any?>? = null,
)