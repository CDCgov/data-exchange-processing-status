package gov.cdc.ocio.database.models

import gov.cdc.ocio.database.dynamo.ReportConverterProvider
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean


/**
 * Report deadletter when there is missing fields or malformed data.
 *
 * @property dispositionType String?
 * @property deadLetterReasons List<String>?
 * @property validationSchemas List<String>?
 */
@DynamoDbBean(converterProviders = [
    ReportConverterProvider::class
])
class ReportDeadLetter : Report() {

    var dispositionType: String? = null

    var deadLetterReasons: List<String>? = null

    var validationSchemas: List<String>? = null
}
