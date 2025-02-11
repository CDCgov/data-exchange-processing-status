package gov.cdc.ocio.reportschemavalidator.loaders

import gov.cdc.ocio.reportschemavalidator.models.ReportSchemaMetadata
import gov.cdc.ocio.reportschemavalidator.models.SchemaFile
import gov.cdc.ocio.reportschemavalidator.models.SchemaLoaderInfo
import gov.cdc.ocio.types.health.HealthCheckSystem


/**
 * The interface which loads the schema files from the class path
 */
interface SchemaLoader {

    val system: String
        get() = "Schema Loader"

    /**
     * Defines the interface for loading a schema file.
     *
     * @param fileName [String]
     * @return [SchemaFile]
     */
    fun loadSchemaFile(fileName: String): SchemaFile

    /**
     * Defines the interface for retrieving a list of the schema files that are available.
     *
     * @return [List]<[ReportSchemaMetadata]>
     */
    fun getSchemaFiles(): List<ReportSchemaMetadata>

    /**
     * Provides the schema loader information.
     *
     * @return [SchemaLoaderInfo]
     */
    fun getInfo(): SchemaLoaderInfo

    /**
     * Get the report schema content from the provided information.
     *
     * @param schemaFilename [String]
     * @return [Map]<[String], [Any]>
     */
    fun getSchemaContent(schemaFilename: String): Map<String, Any>

    /**
     * Get the report schema content from the provided information.
     *
     * @param schemaName [String]
     * @param schemaVersion [String]
     * @return [Map]<[String], [Any]>
     */
    fun getSchemaContent(schemaName: String, schemaVersion: String): Map<String, Any>

    /**
     * Converts the provided schema name and version onto a schema filename.
     *
     * @param schemaName [String]
     * @param schemaVersion [String]
     * @return String
     */
    fun getFilename(schemaName: String, schemaVersion: String) = "$schemaName.$schemaVersion.schema.json"

    /**
     * Upserts a report schema -- if it does not exist it is added, otherwise the schema is replaced.  The schema is
     * validated before it is allowed to be upserted.
     *
     * @param schemaName [String]
     * @param schemaVersion [String]
     * @param content [String]
     * @return [String] - filename of the upserted report schema
     */
    fun upsertSchema(schemaName: String, schemaVersion: String, content: String): String

    /**
     * Removes the schema file associated with the name and version provided.
     *
     * @param schemaName [String]
     * @param schemaVersion [String]
     * @return [String] - filename of the removed report schema
     */
    fun removeSchema(schemaName: String, schemaVersion: String): String

    var healthCheckSystem: HealthCheckSystem
}
