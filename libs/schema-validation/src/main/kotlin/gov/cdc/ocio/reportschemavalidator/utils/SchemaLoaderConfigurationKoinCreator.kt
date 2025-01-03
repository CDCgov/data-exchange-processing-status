package gov.cdc.ocio.reportschemavalidator.utils

import io.ktor.server.application.*
import mu.KotlinLogging
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Helper class for creating koin modules for a report schema loader.
 */
class SchemaLoaderConfigurationKoinCreator {

    companion object {


        /**
         * The class which loads the specific cloud schema loader configuration based on the env vars
         * @param environment ApplicationEnvironment
         * @return SchemaLoader
         */
        fun getSchemaLoaderConfigurationFromAppEnv(environment: ApplicationEnvironment): Module {
            val logger = KotlinLogging.logger {}

            val schemaLoaderSystemModule = module {
                val schemaLoaderSystem = environment.config.property("ktor.report_schema_loader_system").getString()
                val schemaLoaderSystemType: SchemaLoaderSystemType
                when (schemaLoaderSystem.lowercase()) {
                    SchemaLoaderSystemType.S3.toString().lowercase() -> {
                        single {  AWSS3Configuration(environment.config,configurationPath = "aws") }
                        schemaLoaderSystemType = SchemaLoaderSystemType.S3
                    }

                    SchemaLoaderSystemType.BLOB_STORAGE.toString().lowercase() -> {
                        single {  AzureBlobStorageConfiguration(environment.config,configurationPath = "azure") }
                        schemaLoaderSystemType = SchemaLoaderSystemType.BLOB_STORAGE
                    }

                    SchemaLoaderSystemType.FILE_SYSTEM.toString().lowercase() -> {
                        single {  FileSystemConfiguration(environment.config,configurationPath = "file_system") }
                        schemaLoaderSystemType = SchemaLoaderSystemType.FILE_SYSTEM
                    }

                    else -> {
                        val msg = "Unsupported schema loader type: $schemaLoaderSystem"
                        logger.error { msg }
                        throw IllegalArgumentException(msg)
                    }

                }
                single { schemaLoaderSystemType } // add databaseType to Koin Modules
            }
            return schemaLoaderSystemModule
        }
    }
}