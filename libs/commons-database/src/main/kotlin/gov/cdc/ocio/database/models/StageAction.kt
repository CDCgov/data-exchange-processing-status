package gov.cdc.ocio.database.models

/**
 * List of known stage actions used in common report queries.
 *
 * @property value String
 * @constructor
 */
enum class StageAction(private val value: String) {
    METADATA_VERIFY("metadata-verify"),
    UPLOAD_STARTED("upload-started"),
    UPLOAD_STATUS("upload-status"),
    UPLOAD_COMPLETED("upload-completed"),
    FILE_DELIVERY("blob-file-copy"); // terrible name, should be changed to be more generic

    override fun toString(): String {
        return value
    }
}