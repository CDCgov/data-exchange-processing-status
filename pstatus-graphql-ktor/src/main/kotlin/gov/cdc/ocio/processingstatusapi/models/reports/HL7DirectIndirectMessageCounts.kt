package gov.cdc.ocio.processingstatusapi.models.reports

import com.expediagroup.graphql.generator.annotations.GraphQLDescription

@GraphQLDescription("HL7v2 message counts using a direct and indirect counting method")
data class HL7DirectIndirectMessageCounts(

    @GraphQLDescription("The direct counting method is a sum of all the receiver.number_of_messages in the HL7 debatch reports")
    var directCounts: Long? = null,

    @GraphQLDescription("The indirect counting method is a sum of all the redacted reports")
    var indirectCounts: Long? = null,

    @GraphQLDescription("Total time to run the query in milliseconds")
    var queryTimeMillis: Long? = null
)