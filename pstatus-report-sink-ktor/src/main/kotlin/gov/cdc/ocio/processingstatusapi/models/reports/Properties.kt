package gov.cdc.ocio.processingstatusapi.models.reports

import com.google.gson.annotations.SerializedName


/**
 * Get properties from the report message.
 *
 * @property level String?
 * @property message String?

 */
class Properties {

    @SerializedName("level")
    var level : String? = null

    @SerializedName("message")
    var message: String? = null


}