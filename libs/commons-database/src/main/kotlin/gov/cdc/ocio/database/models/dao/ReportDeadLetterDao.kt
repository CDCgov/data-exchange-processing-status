package gov.cdc.ocio.database.models.dao

import gov.cdc.ocio.database.dynamo.ReportConverterProvider
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean


/**
 * Data access object for dead-letter reports, which is the structure returned from CosmosDB queries.
 */
@DynamoDbBean(converterProviders = [
    ReportConverterProvider::class
])
data class ReportDeadLetterDao(

    var dispositionType: String? = null,

    var deadLetterReasons: List<String>? = null,

    var validationSchemas: List<String>? = null,

) : ReportDao()