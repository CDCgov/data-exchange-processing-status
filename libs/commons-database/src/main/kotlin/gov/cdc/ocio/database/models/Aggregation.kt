package gov.cdc.ocio.database.models

import com.google.gson.annotations.SerializedName

/**
 * Issue leve; of Report-ERROR OR WARNING
 */
enum class Aggregation {
    @SerializedName("SINGLE")
    SINGLE,
    @SerializedName("BATCH")
    BATCH
}