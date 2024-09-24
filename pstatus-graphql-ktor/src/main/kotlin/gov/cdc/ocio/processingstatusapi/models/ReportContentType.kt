package gov.cdc.ocio.processingstatusapi.models

/**
 * Enumeration of the possible sort orders for queries.
 */
enum class ReportContentType (val type: String){
    JSON("application/json"),
    JSON_SHORT("json"),
    BASE64("base64");

    companion object {
        fun fromString(type: String): ReportContentType {
            return values().find { it.type.equals(type, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unsupported content type: $type")
        }
    }
}