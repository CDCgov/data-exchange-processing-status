package gov.cdc.ocio.database.models

import gov.cdc.ocio.database.dynamo.ReportConverterProvider
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey
import java.time.Instant


/**
 * Report for a given stage.
 *
 * @property id String?
 * @property uploadId String?
 * @property reportSchemaVersion String?
 * @property reportId String?
 * @property dataStreamId String?
 * @property dataStreamRoute String?
 * @property dexIngestDateTime Instant?
 * @property messageMetadata MessageMetadata?
 * @property stageInfo StageInfo?
 * @property tags Map<String, String>?
 * @property data Map<String, String>?
 * @property contentType String?
 * @property jurisdiction String?
 * @property senderId String?
 * @property dataProducerId String?
 * @property source Source?
 * @property content Any?
 * @property timestamp Instant
 * @constructor
 */
@DynamoDbBean(converterProviders = [
    ReportConverterProvider::class
])
open class Report(

    @get:DynamoDbPartitionKey
    var id : String? = null,

    var reportSchemaVersion:String?=null,

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

    var dataProducerId: String? = null,

    var source: String? = null,

    var content: Any? = null,

    var timestamp: Instant = Instant.now()
)
