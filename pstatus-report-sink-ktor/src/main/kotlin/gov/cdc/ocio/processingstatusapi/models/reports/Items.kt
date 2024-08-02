package gov.cdc.ocio.processingstatusapi.models.reports

import com.google.gson.annotations.SerializedName
/**
 * Get Items from the issues array in report message.
 *
 * @property properties Properties?

 */
class Items {

    @SerializedName("properties")
    var properties : Properties? = null
}