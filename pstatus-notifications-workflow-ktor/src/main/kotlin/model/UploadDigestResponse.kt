package gov.cdc.ocio.processingnotifications.model

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute

@DynamoDbBean
data class UploadDigestResponse(
    @get:DynamoDbPartitionKey
    @get:DynamoDbAttribute("id")
     var id: String="",

    @get:DynamoDbAttribute("jurisdiction")
    var jurisdiction: String = "",

    @get:DynamoDbAttribute("dataStreamId")
    var dataStreamId: String="",

)