package gov.cdc.ocio.processingnotifications.model

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey

/**
 * The POJO class which is used for mapping results from the db
 * @property id String
  */

@DynamoDbBean
data class CheckUploadResponse(
    @get:DynamoDbPartitionKey
    @get:DynamoDbAttribute("id")
    var id: String=""

)
