package gov.cdc.ocio.processingstatusapi.models.reports

import com.google.gson.annotations.SerializedName
import gov.cdc.ocio.database.models.Level
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean


/**
 * Issue leve; of Report-ERROR OR WARNING
 */
enum class Level {
    @SerializedName("ERROR")
    ERROR,
    @SerializedName("WARNING")
    WARNING
}
/**
 * Get issues array in report message.
 *
 * @property level String?
 * @property message String?

 */
@DynamoDbBean
class Issue {

    @SerializedName("level")
    var level : Level? = null

    @SerializedName("message")
    var message: String? = null
}