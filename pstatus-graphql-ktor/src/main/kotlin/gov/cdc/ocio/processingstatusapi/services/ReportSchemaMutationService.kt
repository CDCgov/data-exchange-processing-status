package gov.cdc.ocio.processingstatusapi.services

import com.google.gson.GsonBuilder
import gov.cdc.ocio.processingstatusapi.collections.BasicHashMap
import gov.cdc.ocio.reportschemavalidator.loaders.SchemaLoader
import gov.cdc.ocio.processingstatusapi.models.SchemaActionResult
import gov.cdc.ocio.processingstatusapi.plugins.AuthContext
import gov.cdc.ocio.processingstatusapi.exceptions.InvalidTokenException
import gov.cdc.ocio.processingstatusapi.security.SchemaSecurityConfig
import gov.cdc.ocio.reportschemavalidator.validators.JsonSchemaValidator
import graphql.schema.DataFetchingEnvironment
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


/**
 * Service for managing report schemas.
 *
 * @property schemaLoader SchemaLoader
 * @property gson (Gson..Gson?)
 */
class ReportSchemaMutationService: KoinComponent {

    private val schemaLoader by inject<SchemaLoader>()

    private val schemaSecurityConfig by inject<SchemaSecurityConfig>()

    private val gson = GsonBuilder().setPrettyPrinting().create()

    /**
     * Upserts a report schema -- if it does not exist it is added, otherwise the schema is replaced.  The schema is
     * validated before it is allowed to be upserted.
     *
     * @param schemaName [String]
     * @param schemaVersion [String]
     * @param content [BasicHashMap]<[String], [Any]?>
     * @return [SchemaActionResult]
     */
    fun upsertSchema(
        dataFetchingEnvironment: DataFetchingEnvironment,
        schemaName: String,
        schemaVersion: String,
        content: BasicHashMap<String, Any?>
    ): SchemaActionResult {
        // Make sure the caller has provided a valid token
        verifyAuth(dataFetchingEnvironment)

        // Validate the schema name and version
        require(schemaName.isNotBlank()) { "Schema name is required." }
        require(schemaVersion.isNotBlank()) { "Schema version is required." }
        require(isValidSchemaName(schemaName)) { "Schema name contains invalid characters." }
        require(isValidSchemaVersion(schemaVersion)) { "Schema version contains invalid characters." }

        // Validate the content before upserting it
        val requiredFields = listOf("schema", "id", "title", "type", "defs")
        requiredFields.forEach { fieldName ->
            if (!content.containsKey(fieldName)) throw Exception("Schema file is missing required field, '$fieldName'.")
        }

        val hashMapContent = content.toHashMap().toMutableMap()
        // Replace the special field names with the special characters
        val dollarFieldNames = listOf("schema", "id", "defs")
        dollarFieldNames.forEach { fieldName ->
            if (hashMapContent.containsKey(fieldName)) {
                hashMapContent["\$$fieldName"] = hashMapContent[fieldName]
                hashMapContent.remove(fieldName)
            }
        }

        val contentJson = gson.toJson(hashMapContent)

        // Further validation with the schema validator
        try {
            JsonSchemaValidator().checkSchemaFile(contentJson)
        } catch (ex: Exception) {
            throw Exception("Schema file has issue: ${ex.localizedMessage}")
        }

        val filename = schemaLoader.upsertSchema(schemaName, schemaVersion, contentJson)
        return SchemaActionResult("Upsert schema file", "Success", filename)
    }

    /**
     * Removes the schema file associated with the name and version provided.
     *
     * @param schemaName [String]
     * @param schemaVersion [String]
     * @return [SchemaActionResult]
     */
    fun removeSchema(
        dataFetchingEnvironment: DataFetchingEnvironment,
        schemaName: String,
        schemaVersion: String
    ): SchemaActionResult {
        // Make sure the caller has provided a valid token
        verifyAuth(dataFetchingEnvironment)

        val filename = schemaLoader.removeSchema(schemaName, schemaVersion)
        return SchemaActionResult("Remove schema file", "Success", filename)
    }

    /**
     * Verify the caller has the permissions to use this mutation.
     *
     * @param dataFetchingEnvironment DataFetchingEnvironment
     * @throws InvalidTokenException - if the token is invalid, missing, or doesn't match the expected one
     */
    @Throws(InvalidTokenException::class)
    private fun verifyAuth(dataFetchingEnvironment: DataFetchingEnvironment) {
        val authorization = dataFetchingEnvironment.graphQlContext.get<AuthContext>("Authorization")
        val receivedSchemaAuthToken = authorization?.token

        // Enforce token validation only for this mutation
        if (receivedSchemaAuthToken.isNullOrBlank() || receivedSchemaAuthToken != "Bearer ${schemaSecurityConfig.token}") {
            throw InvalidTokenException("Unauthorized: Missing or invalid bearer token")
        }
    }

    /**
     * Validates whether the provided schema name conforms to the expected naming constraints.
     * A schema name is considered valid if it does not contain folder traversal patterns,
     * control characters, dots, slashes, backslashes, or other invalid characters.
     *
     * @param name The schema name to validate as a [String].
     * @return A [Boolean] indicating whether the schema name is valid. Returns `true` if the name
     *         is valid, and `false` otherwise.
     */
    private fun isValidSchemaName(name: String): Boolean {
        // Reject empty strings and names with control chars, dots, slashes, or backslashes
        if (hasFolderTraversal(name)) return false
        val invalidChars = Regex("""[^\p{L}\p{N}\p{S}\p{P}&&[^.]]""")
        return !invalidChars.containsMatchIn(name)
    }

    /**
     * Validates whether a given schema version follows the expected versioning pattern.
     * A schema version is considered valid if it adheres to a standard major.minor.patch format (e.g., "1.0.0"),
     * and does not contain folder traversal patterns such as "..", forward slashes, or backslashes.
     *
     * @param version The schema version to validate as a [String].
     * @return A [Boolean] indicating whether the schema version is valid. Returns `true` if the version
     *         is valid, and `false` otherwise.
     */
    private fun isValidSchemaVersion(version: String): Boolean {
        if (hasFolderTraversal(version)) return false
        val versionRegex = Regex("""^\d+\.\d+\.\d+$""")
        return versionRegex.containsMatchIn(version)
    }

    /**
     * Checks if the given string contains folder traversal patterns such as "..", ends with a dot,
     * or includes directory separators like "/" or "\".
     *
     * @param str The string to check for folder traversal patterns.
     * @return True if the string contains folder traversal patterns, false otherwise.
     */
    private fun hasFolderTraversal(str: String): Boolean {
        // Check for folder traversal attempts like ".." or ".hidden" at the beginning
        return str.contains("..") || str.endsWith(".") || str.contains("/") || str.contains("\\")
    }
}