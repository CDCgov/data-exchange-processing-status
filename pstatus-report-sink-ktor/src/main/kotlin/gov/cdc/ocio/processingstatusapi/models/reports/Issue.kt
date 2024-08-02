package gov.cdc.ocio.processingstatusapi.models.reports

import com.google.gson.annotations.SerializedName
/**
 * Get Items from the issues array in report message.
 *
 * @property items Items?

 */
class Issue {

    @SerializedName("items")
    var items : Items? = null



}