package gov.cdc.ocio.reportschemavalidator.loaders

import com.fasterxml.jackson.databind.ObjectMapper
import gov.cdc.ocio.reportschemavalidator.models.ReportSchemaMetadata
import gov.cdc.ocio.reportschemavalidator.models.SchemaFile
import gov.cdc.ocio.reportschemavalidator.models.SchemaLoaderInfo
import gov.cdc.ocio.reportschemavalidator.utils.DefaultJsonUtils


/**
 * The class which loads the schema files from the class path
 */
class FileSchemaLoader : SchemaLoader {

    private val schemaDirectoryPath = "schema"

    /**
     * The function which loads the schema based on the file name path and returns a [SchemaFile]
     * @param fileName String
     * @return [SchemaFile]
     */
    override fun loadSchemaFile(fileName: String): SchemaFile {
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
    override fun getInfo() = SchemaLoaderInfo("resources", schemaDirectoryPath)

    /**
     * Get the report schema content from the provided information.
     *
     * @param schemaFilename [String]
     * @return [Map]<[String], [Any]>
     */
    override fun getSchemaContent(schemaFilename: String): Map<String, Any> {
        javaClass.classLoader.getResourceAsStream("$schemaDirectoryPath/$schemaFilename")?.use { inputStream ->
            val jsonContent = inputStream.readAllBytes().decodeToString()
            return DefaultJsonUtils(ObjectMapper()).getJsonMapOfContent(jsonContent)
        }
        return mapOf()
    }

    /**
     * Get the report schema content from the provided information.
     *
     * @param schemaName [String]
     * @param schemaVersion [String]
     * @return [Map]<[String], [Any]>
     */
    override fun getSchemaContent(schemaName: String, schemaVersion: String): Map<String, Any> {
        return getSchemaContent("$schemaName.$schemaVersion.schema.json")
    }

}
