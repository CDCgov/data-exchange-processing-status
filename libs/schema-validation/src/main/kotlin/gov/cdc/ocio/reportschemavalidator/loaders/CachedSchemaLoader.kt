package gov.cdc.ocio.reportschemavalidator.loaders

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import gov.cdc.ocio.reportschemavalidator.models.ReportSchemaMetadata
import gov.cdc.ocio.reportschemavalidator.models.SchemaFile
import gov.cdc.ocio.reportschemavalidator.models.SchemaLoaderInfo
import gov.cdc.ocio.reportschemavalidator.utils.DefaultJsonUtils
import java.util.concurrent.TimeUnit


/**
 * Cached schema loader intended to wrap the backing schema loader; e.g. file_system, s3, blob_storage.
 *
 * @property schemaLoaderImpl [SchemaLoader]
 * @property schemaContentCache [com.google.common.cache.LoadingCache]<[String], [SchemaFile]>
 * @property schemaFileListCache [com.google.common.cache.LoadingCache]<[String], [List]<[ReportSchemaMetadata]>>
 * @constructor
 */
class CachedSchemaLoader(private val schemaLoaderImpl: SchemaLoader) : SchemaLoader {

    companion object {
        // Evict report schema content after a reasonable period of time even though in theory, report content should
        // NEVER change.
        private const val CACHED_SCHEMA_FILE_CONTENT_DURATION_MINUTES = 15L

        // The list of report schema files can change often, so expire the list after a short period of time.
        private const val CACHED_SCHEMA_FILE_LIST_DURATION_MINUTES = 5L
    }

    private val schemaContentCache = CacheBuilder.newBuilder()
        .expireAfterWrite(CACHED_SCHEMA_FILE_CONTENT_DURATION_MINUTES, TimeUnit.MINUTES) // Expire entries after 15 minutes
        .build(
            object : CacheLoader<String, SchemaFile>() {
                override fun load(fileName: String): SchemaFile {
                    return schemaLoaderImpl.loadSchemaFile(fileName)
                }
            }
        )

    // Note: The "memoized" guava is another option in lieu of cache, but it's not a great one for our needs.
    // Although memoized entries are a keyless cache, it requires you to have a static function you call to get the
    // value, which isn't ideal for our purpose.
    private val schemaFileListCache = CacheBuilder.newBuilder()
        .expireAfterWrite(CACHED_SCHEMA_FILE_LIST_DURATION_MINUTES, TimeUnit.MINUTES)
        .build(
            object : CacheLoader<Int, List<ReportSchemaMetadata>>() {
                override fun load(unused : Int): List<ReportSchemaMetadata> {
                    return schemaLoaderImpl.getSchemaFiles()
                }
            }
        )

    /**
     * Provides the cached schema based on the file name path and returns a [SchemaFile].
     *
     * @param fileName String
     * @return [SchemaFile]
     */
    override fun loadSchemaFile(fileName: String): SchemaFile = schemaContentCache.get(fileName)

    /**
     * Provides the cached list of schema files that are available.
     *
     * @return List<[ReportSchemaMetadata]>
     */
    override fun getSchemaFiles(): List<ReportSchemaMetadata> = schemaFileListCache.get(0)

    /**
     * Provides the cached schema loader information.
     *
     * @return SchemaLoaderInfo
     */
    override fun getInfo(): SchemaLoaderInfo = schemaLoaderImpl.getInfo() // No need to cache

    /**
     * Provides the cached report schema content from the provided information.
     *
     * @param schemaFilename [String]
     * @return [Map]<[String], [Any]>
     */
    override fun getSchemaContent(schemaFilename: String): Map<String, Any> {
        val jsonContent = schemaContentCache.get(schemaFilename).content ?: ""
        return DefaultJsonUtils(ObjectMapper()).getJsonMapOfContent(jsonContent)
    }

    /**
     * Provides the cached report schema content from the provided information.
     *
     * @param schemaName [String]
     * @param schemaVersion [String]
     * @return [Map]<[String], [Any]>
     */
    override fun getSchemaContent(schemaName: String, schemaVersion: String): Map<String, Any> =
        getSchemaContent("$schemaName.$schemaVersion.schema.json")

    override var healthCheckSystem = schemaLoaderImpl.healthCheckSystem
}