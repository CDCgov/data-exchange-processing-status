package gov.cdc.ocio.processingstatusapi.models

import gov.cdc.ocio.processingstatusapi.dynamo.Content
import gov.cdc.ocio.processingstatusapi.dynamo.ContentConverterProvider
import gov.cdc.ocio.processingstatusapi.models.reports.MessageMetadata
import gov.cdc.ocio.processingstatusapi.models.reports.StageInfo
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey
import java.time.Instant


/**
 * Report for a given stage.
 *
 * @property id String?
 * @property uploadId String?
 * @property reportId String?
 * @property dataStreamId String?
 * @property dataStreamRoute String?
 * @property dexIngestDateTime Date?
 * @property messageMetadata MessageMetadata?
 * @property stageInfo StageInfo?
 * @property tags Map<String, String>?
 * @property data Map<String, String>?
 * @property contentType String?
 * @property jurisdiction String?
 * @property senderId String?
 * @property content Any?
 * @property timestamp Date
 * @constructor
 */
@DynamoDbBean(converterProviders = [
    ContentConverterProvider::class,
//    DateConverterProvider::class
])
open class Report(

    @get:DynamoDbPartitionKey
    var id : String? = null,

    var uploadId: String? = null,

    var reportId: String? = null,

    var dataStreamId: String? = null,

    var dataStreamRoute: String? = null,

    var dexIngestDateTime: Instant? = null,

    var messageMetadata: MessageMetadata? = null,

    var stageInfo: StageInfo? = null,

    var tags: Map<String, String>? = null,

    var data: Map<String, String>? = null,

    var contentType : String? = null,

    var jurisdiction: String? = null,

    var senderId: String? = null,

    var content: Content? = null,
//    var content: Map<String, Any>? = null,

    val timestamp: Instant = Instant.now()
)
