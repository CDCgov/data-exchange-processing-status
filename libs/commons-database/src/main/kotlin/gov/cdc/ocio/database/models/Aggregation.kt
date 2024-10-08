package gov.cdc.ocio.database.models

import com.google.gson.annotations.SerializedName

/**
 * Indicated the type of aggregation for the report.  Single indicates the upload maps to only one item whereas batch
 * means the upload maps to multiple items.
 */
enum class Aggregation {
    @SerializedName("SINGLE")
    SINGLE,
    @SerializedName("BATCH")
    BATCH
}