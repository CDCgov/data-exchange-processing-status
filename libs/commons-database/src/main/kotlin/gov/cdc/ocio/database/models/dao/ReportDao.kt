package gov.cdc.ocio.database.models.dao

import com.google.gson.Gson
import gov.cdc.ocio.database.dynamo.ReportConverterProvider
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import java.time.Instant
import java.util.*


/**
 * Data access object for reports, which is the structure returned from CosmosDB queries.
 *
 * @property id String?
 * @property uploadId String?
 * @property reportId String?
 * @property dataStreamId String?
 * @property dataStreamRoute String?
 * @property dexIngestDateTime Date?
 * @property messageMetadata MessageMetadata?
 * @property stageInfo StageInfo?
 * @property tags Map<String,String??
 * @property data Map<String,String>?
 * @property contentType String?
 * @property timestamp Date?
 * @property content Any?
 * @property jurisdiction String?
 * @property senderId String?
 * @property dataProducerId String?
 * @property contentAsString String?
 * @constructor
 */
@DynamoDbBean(converterProviders = [
    ReportConverterProvider::class
])
open class ReportDao(

    var id : String? = null,

    var uploadId: String? = null,

    var reportId: String? = null,

    var dataStreamId: String? = null,

    var dataStreamRoute: String? = null,

    var dexIngestDateTime: Instant? = null,

    var messageMetadata: MessageMetadataDao? = null,

    var stageInfo: StageInfoDao? = null,

    var tags: Map<String, String>? = null,

    var data: Map<String, String>? = null,

    var contentType: String? = null,

    var jurisdiction: String? = null,

    var senderId: String? = null,

    var dataProducerId: String? = null,

    var source: String? = null,

    var content: Any? = null,

    var timestamp: Instant = Instant.now()
) {

    val contentAsString: String?
        get() {
            if (content == null) return null

            return when (contentType?.lowercase(Locale.getDefault())) {
                in setOf("application/json", "json") -> {
                    if (content is Map<*, *>) {
                        Gson().toJson(content, MutableMap::class.java).toString()
                    } else {
                        content.toString()
                    }
                }
                else -> content.toString()
            }

        }
}