package gov.cdc.ocio.reportschemavalidator.loaders

import gov.cdc.ocio.reportschemavalidator.models.ReportSchemaMetadata
import gov.cdc.ocio.reportschemavalidator.models.SchemaFile
import gov.cdc.ocio.reportschemavalidator.models.SchemaLoaderInfo


/**
 * The class which loads the schema files from the class path
 */
class FileSchemaLoader : SchemaLoader {

    /**
     * The function which loads the schema based on the file name path and returns a [SchemaFile]
     * @param fileName String
     * @return [SchemaFile]
     */
    override fun loadSchemaFile(fileName: String): SchemaFile {
        val schemaDirectoryPath = "schema"
        return SchemaFile(
            fileName = fileName,
            inputStream = javaClass.classLoader.getResourceAsStream("$schemaDirectoryPath/$fileName")
        )
    }

    /**
     * Provides a list of the schema files that are available.
     *
     * @return List<[ReportSchemaMetadata]>
     */
    override fun getSchemaFiles(): List<ReportSchemaMetadata> {
        val resources = javaClass.classLoader.getResources("schema").toList()
        return resources.map { ReportSchemaMetadata(it.file.toString(), "", "", "") }
    }

    /**
     * Provides the schema loader information.
     *
     * @return SchemaLoaderInfo
     */
    override fun getInfo(): SchemaLoaderInfo {
        return SchemaLoaderInfo("resources", "schema")
    }

}
