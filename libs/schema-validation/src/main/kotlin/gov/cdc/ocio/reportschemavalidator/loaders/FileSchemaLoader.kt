package gov.cdc.ocio.reportschemavalidator.loaders

import com.fasterxml.jackson.databind.ObjectMapper
import gov.cdc.ocio.reportschemavalidator.health.schemaLoadersystem.HealthCheckFileSystem
import gov.cdc.ocio.reportschemavalidator.models.ReportSchemaMetadata
import gov.cdc.ocio.reportschemavalidator.models.SchemaFile
import gov.cdc.ocio.reportschemavalidator.models.SchemaLoaderInfo
import gov.cdc.ocio.reportschemavalidator.utils.DefaultJsonUtils
import gov.cdc.ocio.types.health.HealthCheckSystem
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
        val file = java.io.File("$schemaLocalSystemFilePath/$fileName")
        if (!file.exists()) {
            throw FileNotFoundException("Report rejected: file - $fileName not found for content schema.")
        }
        return SchemaFile(
            fileName = fileName,
            content = file.inputStream().readAllBytes().decodeToString()
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
        val file = java.io.File("$schemaLocalSystemFilePath/$schemaFilename")
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
        return getSchemaContent("$schemaName.$schemaVersion.schema.json")
    }

    override var healthCheckSystem = HealthCheckFileSystem() as HealthCheckSystem
}
