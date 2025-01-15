package gov.cdc.ocio.reportschemavalidator.loaders

import gov.cdc.ocio.reportschemavalidator.models.ReportSchemaMetadata
import gov.cdc.ocio.reportschemavalidator.models.SchemaFile
import gov.cdc.ocio.reportschemavalidator.models.SchemaLoaderInfo
import gov.cdc.ocio.types.health.HealthCheckSystem


/**
 * The interface which loads the schema files from the class path
 */
interface SchemaLoader {

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

    var healthCheckSystem: HealthCheckSystem
}
