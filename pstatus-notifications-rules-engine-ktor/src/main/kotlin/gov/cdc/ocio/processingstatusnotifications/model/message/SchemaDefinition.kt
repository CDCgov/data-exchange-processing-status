package gov.cdc.ocio.processingstatusnotifications.model.message

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import gov.cdc.ocio.processingstatusnotifications.exception.InvalidSchemaDefException


/**
 * Schema definition for all stages.  Every stage must inherit this class.
 *
 * @property contentSchemaName String?
 * @property contentSchemaVersion String?
 * @constructor
 */
open class SchemaDefinition(
    @SerializedName("content_schema_name") var contentSchemaName: String? = null,
    @SerializedName("content_schema_version") var contentSchemaVersion: String? = null,
    @Transient private val priority: Int = 0
) :
    Comparable<SchemaDefinition> {

    /**
     * Hash operator
     *
     * @return Int
     */
    override fun hashCode(): Int {
        var result = contentSchemaName?.hashCode() ?: 0
        result = 31 * result + (contentSchemaVersion?.hashCode() ?: 0)
        return result
    }

    /**
     * Compare operator
     *
     * @param other SchemaDefinition
     * @return Int
     */
    override fun compareTo(other: SchemaDefinition): Int {
        return compareValues(priority, other.priority)
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

        if (contentSchemaName != other.contentSchemaName) return false
        if (contentSchemaVersion != other.contentSchemaVersion) return false

        return true
    }

    /**
     * Return the schema definition as a human-readable string.
     *
     * @return String
     */
    override fun toString(): String {
        return "SchemaDefinition(contentSchemaName=$contentSchemaName, contentSchemaVersion=$contentSchemaVersion)"
    }

    companion object {

        @Throws(InvalidSchemaDefException::class)
        fun fromJsonString(jsonContent: String?): SchemaDefinition {
            if (jsonContent == null) throw InvalidSchemaDefException("Missing schema definition")

            val schemaDefinition = Gson().fromJson(jsonContent, SchemaDefinition::class.java)
            if (schemaDefinition?.contentSchemaName.isNullOrEmpty())
                throw InvalidSchemaDefException("Invalid content_schema_name provided")

            if (schemaDefinition.contentSchemaVersion.isNullOrEmpty())
                throw InvalidSchemaDefException("Invalid content_schema_version provided")

            return schemaDefinition
        }
    }
}
