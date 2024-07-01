package gov.cdc.ocio.processingstatusapi.models.reports

import com.expediagroup.graphql.generator.annotations.GraphQLDescription

@GraphQLDescription("HL7v2 invalid message counts using a direct and indirect counting method")
data class HL7InvalidMessageCounts(

    @GraphQLDescription("The invalid message direct counting method is a sum of the redacted messages that were not propagated and the structure validator reports that are invalid")
    var invalidMessageDirectCounts: Long? = null,

    @GraphQLDescription("The invalid message indirect counting method is the total of the HL7-JSON-Lake-Transformer that are not present or HL7-JSON-Lake-Transformer < Structure-Validator")
    var invalidMessageIndirectCounts: Long? = null,

    @GraphQLDescription("Total time to run the query in milliseconds")
    var queryTimeMillis: Long? = null
)