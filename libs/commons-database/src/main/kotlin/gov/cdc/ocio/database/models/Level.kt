package gov.cdc.ocio.database.models

import com.google.gson.annotations.SerializedName

/**
 * Issue level of Report-ERROR OR WARNING
 */
enum class Level {
    @SerializedName("ERROR")
    ERROR,
    @SerializedName("WARNING")
    WARNING
}