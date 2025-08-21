package gov.cdc.ocio.database.models

/**
 * List of known stage services used in common report queries.
 *
 * @property value String
 * @constructor
 */
enum class StageService(private val value: String) {
    UPLOAD_API("UPLOAD API");

    override fun toString(): String {
        return value
    }
}
