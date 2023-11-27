package gov.cdc.ocio.processingstatusapi.model.stagereports

/**
 * Schema definition for all stages.  Every stage must inherit this class.
 *
 * @property schemaName String?
 * @property schemaVersion String?
 * @constructor
 */
open class SchemaDefinition(var schemaName: String? = null, var schemaVersion: String? = null) {

    /**
     * Hash operator
     *
     * @return Int
     */
    override fun hashCode(): Int {
        var result = schemaName?.hashCode() ?: 0
        result = 31 * result + (schemaVersion?.hashCode() ?: 0)
        return result
    }

    /**
     * Equals operator
     *
     * @param other Any?
     * @return Boolean
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SchemaDefinition

        if (schemaName != other.schemaName) return false
        if (schemaVersion != other.schemaVersion) return false

        return true
    }

    /**
     * Return the schema definition as a human-readable string.
     *
     * @return String
     */
    override fun toString(): String {
        return "SchemaDefinition(schemaName=$schemaName, schemaVersion=$schemaVersion)"
    }
}