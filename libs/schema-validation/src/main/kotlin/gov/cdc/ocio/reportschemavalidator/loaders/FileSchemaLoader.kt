package gov.cdc.ocio.reportschemavalidator.loaders

import com.fasterxml.jackson.databind.ObjectMapper
import gov.cdc.ocio.reportschemavalidator.exceptions.SchemaNotFoundException
import gov.cdc.ocio.reportschemavalidator.health.schemaLoadersystem.HealthCheckFileSystem
import gov.cdc.ocio.reportschemavalidator.models.ReportSchemaMetadata
import gov.cdc.ocio.reportschemavalidator.models.SchemaFile
import gov.cdc.ocio.reportschemavalidator.models.SchemaLoaderInfo
import gov.cdc.ocio.reportschemavalidator.utils.DefaultJsonUtils
import gov.cdc.ocio.types.health.HealthCheckSystem
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.inputStream
import kotlin.io.path.listDirectoryEntries


/**
 * The class which loads the schema files from the class path
 */
class FileSchemaLoader(
    config: Map<String, String>
) : SchemaLoader {

    private val schemaLocalSystemFilePath = config["REPORT_SCHEMA_LOCAL_FILE_SYSTEM_PATH"]
        ?: throw IllegalArgumentException("Local file system path is not configured")

    /**
     * The function which loads the schema based on the file name path and returns a [SchemaFile]
     * @param fileName String
     * @return [SchemaFile]
     */
    override fun loadSchemaFile(fileName: String): SchemaFile {
        val file = File("$schemaLocalSystemFilePath/$fileName")

        val content = if (file.exists())
            file.inputStream().readAllBytes().decodeToString()
        else throw SchemaNotFoundException(file.path)

        return SchemaFile(
            fileName,
            content
        )
    }

    /**
     * Provides a list of the schema files that are available.
     *
     * @return List<[ReportSchemaMetadata]>
     */
    override fun getSchemaFiles(): List<ReportSchemaMetadata> {
        val folderPath = Paths.get(schemaLocalSystemFilePath)
        val files = folderPath.listDirectoryEntries().filter { Files.isRegularFile(it) && it.toFile().extension == "json" }

        return files.map { filePath ->
            filePath.inputStream().use { content ->
                ReportSchemaMetadata.from(
                    filePath.toFile().name,
                    content.readAllBytes().decodeToString()
                )
            }
        }
    }

    /**
     * Provides the schema loader information.
     *
     * @return SchemaLoaderInfo
     */
    override fun getInfo() = SchemaLoaderInfo("file_system", schemaLocalSystemFilePath)

    /**
     * Get the report schema content from the provided information.
     *
     * @param schemaFilename [String]
     * @return [Map]<[String], [Any]>
     */
    override fun getSchemaContent(schemaFilename: String): Map<String, Any> {
        val file = File("$schemaLocalSystemFilePath/$schemaFilename")

        file.inputStream().use { inputStream ->
            val jsonContent = inputStream.readAllBytes().decodeToString()
            return DefaultJsonUtils(ObjectMapper()).getJsonMapOfContent(jsonContent)
        }
    }

    /**
     * Get the report schema content from the provided information.
     *
     * @param schemaName [String]
     * @param schemaVersion [String]
     * @return [Map]<[String], [Any]>
     */
    override fun getSchemaContent(schemaName: String, schemaVersion: String): Map<String, Any> {
        return getSchemaContent(getFilename(schemaName, schemaVersion))
    }

    /**
     * Upserts a report schema -- if it does not exist it is added, otherwise the schema is replaced.  The schema is
     * validated before it is allowed to be upserted.
     *
     * @param schemaName [String]
     * @param schemaVersion [String]
     * @param content [String]
     * @return [String] - filename of the upserted report schema
     */
    override fun upsertSchema(schemaName: String, schemaVersion: String, content: String): String {
        val schemaFilename = getFilename(schemaName, schemaVersion)
        val file = File(getSchemaFilePathName(schemaFilename))
        file.writeText(content)
        return schemaFilename
    }

    /**
     * Removes the schema file associated with the name and version provided.
     *
     * @param schemaName [String]
     * @param schemaVersion [String]
     * @return [String] - filename of the removed report schema
     */
    override fun removeSchema(schemaName: String, schemaVersion: String): String {
        val schemaFilename = getFilename(schemaName, schemaVersion)
        val file = File(getSchemaFilePathName(schemaFilename))
        var fileDeleted = false
        if (file.exists())
            fileDeleted = file.delete()
        if (!fileDeleted)
            throw FileNotFoundException("Schema file not found or could not be deleted: "
                + "$schemaFilename for schema: $schemaName, schemaVersion: $schemaVersion")
        return schemaFilename
    }

    private fun getSchemaFilePathName(schemaFilename: String) = "$schemaLocalSystemFilePath/$schemaFilename"

    override var healthCheckSystem = HealthCheckFileSystem(system, schemaLocalSystemFilePath) as HealthCheckSystem
}
