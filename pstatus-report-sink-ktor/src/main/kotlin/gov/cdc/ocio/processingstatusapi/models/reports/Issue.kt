package gov.cdc.ocio.processingstatusapi.models.reports

import com.google.gson.annotations.SerializedName


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
class Issue {

    @SerializedName("level")
    var level : Level? = null

    @SerializedName("message")
    var message: String? = null
}