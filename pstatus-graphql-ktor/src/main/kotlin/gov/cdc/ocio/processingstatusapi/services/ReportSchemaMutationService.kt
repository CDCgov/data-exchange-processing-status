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
     * Validates whether a given schema name is valid. A schema name
     * is considered valid if it does not contain certain invalid characters
     * (control characters, dots, forward slashes, or backslashes) or folder
     * traversal patterns.
     *
     * @param name The schema name to validate.
     * @return True if the schema name is valid, false otherwise.
     */
    private fun isValidSchemaName(name: String): Boolean {
        // Reject empty strings and names with control chars, dots, slashes, or backslashes
        val invalidChars = Regex("""[./\\\p{Cntrl}]""")
        if (hasFolderTraversal(name)) return false
        return !invalidChars.containsMatchIn(name)
    }

    /**
     * Validates the given schema version string to ensure it does not contain
     * invalid characters or folder traversal patterns.
     *
     * @param version The schema version string to validate.
     * @return True if the schema version is valid, false otherwise.
     */
    private fun isValidSchemaVersion(version: String): Boolean {
        // Disallow slashes, backslashes, control characters
        val invalidChars = Regex("""[/\\\p{Cntrl}]""")
        if (hasFolderTraversal(version)) return false
        return !invalidChars.containsMatchIn(version)
    }

    /**
     * Checks if the provided string contains folder traversal patterns.
     *
     * @param str The string to check for folder traversal.
     *            This can include patterns such as "..", ".hidden", or paths indicating parent directory traversal.
     * @return True if the string contains folder traversal sequences, false otherwise.
     */
    private fun hasFolderTraversal(str: String): Boolean {
        // Check for folder traversal attempts like ".." or ".hidden" at the beginning
        return str == "." || str == ".." || str.startsWith("..") || str.contains("/../") || str.contains("\\..\\")
    }
}