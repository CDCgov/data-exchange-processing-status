package gov.cdc.ocio.processingstatusapi.model.reports

import com.google.gson.annotations.SerializedName

/**
 * Aggregate summary of report counts
 *
 * @property numUploads Int?
 * @constructor
 */
data class AggregateSummary(

    @SerializedName("num_uploads")
    var numUploads: Int? = null,
)