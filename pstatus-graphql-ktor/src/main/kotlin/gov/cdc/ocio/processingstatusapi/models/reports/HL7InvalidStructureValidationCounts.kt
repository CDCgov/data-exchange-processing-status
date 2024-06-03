package gov.cdc.ocio.processingstatusapi.models.reports

import com.expediagroup.graphql.generator.annotations.GraphQLDescription

@GraphQLDescription("Counts the number of HL7v2 structures that are invalid for the provided parameters")
data class HL7InvalidStructureValidationCounts(

    @GraphQLDescription("Count of HL7v2 messages with an invalid structure")
    var counts: Long? = null,

    @GraphQLDescription("Total time to run the query in milliseconds")
    var queryTimeMillis: Long? = null
)