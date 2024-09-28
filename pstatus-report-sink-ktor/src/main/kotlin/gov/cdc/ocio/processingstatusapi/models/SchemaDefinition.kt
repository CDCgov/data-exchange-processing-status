package gov.cdc.ocio.processingstatusapi.models

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import gov.cdc.ocio.processingstatusapi.exceptions.InvalidSchemaDefException

/**
 * Schema definition for all stages.  Every stage must inherit this class.
 *
 * @property schemaName String?
 * @property schemaVersion String?
 * @constructor
 */
open class SchemaDefinition(@SerializedName("schema_name") var schemaName: String? = null,
                            @SerializedName("schema_version") var schemaVersion: String? = null,
                            @Transient private val priority: Int = 0) :
    Comparable<SchemaDefinition> {

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

    companion object {

        @Throws(InvalidSchemaDefException::class)
        fun fromJsonString(jsonContent: Any?): SchemaDefinition {
            if (jsonContent == null) throw InvalidSchemaDefException("Missing schema definition")
           val schemaDefinition= Gson().fromJson(Gson().toJson(jsonContent, MutableMap::class.java).toString(), SchemaDefinition::class.java)
            if (schemaDefinition?.schemaName.isNullOrEmpty())
                throw InvalidSchemaDefException("Invalid schema_name provided")

            if (schemaDefinition.schemaVersion.isNullOrEmpty())
                throw InvalidSchemaDefException("Invalid schema_version provided")

            return schemaDefinition
        }
    }
}