package gov.cdc.ocio.reportschemavalidator.loaders

import gov.cdc.ocio.reportschemavalidator.models.ReportSchemaMetadata
import gov.cdc.ocio.reportschemavalidator.models.SchemaFile
import gov.cdc.ocio.reportschemavalidator.models.SchemaLoaderInfo


/**
 * The interface which loads the schema files from the class path
 */
interface SchemaLoader {

    /**
     * Defines the interface for loading a schema file.
     *
     * @param fileName String
     * @return SchemaFile
     */
    fun loadSchemaFile(fileName: String): SchemaFile

    /**
     * Defines the interface for retrieving a list of the schema files that are available.
     *
     * @return List<[ReportSchemaMetadata]>
     */
    fun getSchemaFiles(): List<ReportSchemaMetadata>

    /**
     * Provides the schema loader information.
     *
     * @return SchemaLoaderInfo
     */
    fun getInfo(): SchemaLoaderInfo
}
