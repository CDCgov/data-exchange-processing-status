package gov.cdc.ocio.reportschemavalidator.utils

import gov.cdc.ocio.reportschemavalidator.health.schemaLoadersystem.modules.SchemaLoaderModules
import io.ktor.server.application.*
import io.ktor.server.config.*
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
                        single { AWSS3Configuration(environment.config ,configurationPath = "aws") }
                        schemaLoaderSystemType = SchemaLoaderSystemType.S3
                    }

                    SchemaLoaderSystemType.BLOB_STORAGE.toString().lowercase() -> {
                        single { AzureBlobStorageConfiguration(environment.config, configurationPath = "azure") }
                        schemaLoaderSystemType = SchemaLoaderSystemType.BLOB_STORAGE
                    }

                    SchemaLoaderSystemType.FILE_SYSTEM.toString().lowercase() -> {
                        single { FileSystemConfiguration(environment.config, configurationPath = "file_system") }
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
        /**
         *  Set the appropriate schema loader module
         *  Creates a koin module and injects singletons for the schema loader config specified in the [ApplicationEnvironment]
         *  @param environment ApplicationEnvironment
         * @return [Module] Resultant koin module.
         */
        fun schemaLoaderHealthCheckModuleFromAppEnv(environment: ApplicationEnvironment):Module {
            val logger = KotlinLogging.logger {}
            val schemaLoaderSystem = environment.config.property("ktor.report_schema_loader_system").getString()
            var schemaLoaderSystemType:SchemaLoaderSystemType = SchemaLoaderSystemType.FILE_SYSTEM
            when (schemaLoaderSystem.lowercase()) {
                SchemaLoaderSystemType.S3.toString().lowercase() -> {
                    schemaLoaderSystemType = SchemaLoaderSystemType.S3
                    return SchemaLoaderModules.provideS3Module(
                        config = environment.config,
                        region = environment.config.tryGetString("aws.s3.report_schema_region") ?: "",
                       /* roleArn = environment.config.tryGetString("aws.role_arn") ?: "",
                        webIdentityTokenFile = environment.config.tryGetString("aws.web_identity_token_file") ?: ""*/

                    )

                }

                SchemaLoaderSystemType.BLOB_STORAGE.toString().lowercase() -> {
                    schemaLoaderSystemType = SchemaLoaderSystemType.BLOB_STORAGE
                    return  SchemaLoaderModules.provideBlobContainerModule(
                        config= environment.config,
                        connectionString = environment.config.tryGetString("azure.blob_storage.report_schema_connection_string") ?: "",
                        container =  environment.config.tryGetString("azure.blob_storage.report_schema_container") ?: ""
                                            )
                }


                else -> logger.error("Unsupported schema loader system requested: $schemaLoaderSystemType")
            }

            return module { single { schemaLoaderSystemType } }
        }
    }
}