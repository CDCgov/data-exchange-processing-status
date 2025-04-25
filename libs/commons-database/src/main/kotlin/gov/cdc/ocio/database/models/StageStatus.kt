package gov.cdc.ocio.database.models

/**
 * List of known stage statuses used in common report queries.
 *
 * @property value String
 * @constructor
 */
enum class StageStatus(private val value: String) {
    SUCCESS("SUCCESS"),
    FAILURE("FAILURE");

    override fun toString(): String {
        return value
    }
}