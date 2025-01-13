package gov.cdc.ocio.reportschemavalidator.health.schemaLoadersystem.modules

import gov.cdc.ocio.reportschemavalidator.health.schemaLoadersystem.HealthCheckBlobContainer
import gov.cdc.ocio.reportschemavalidator.health.schemaLoadersystem.HealthCheckS3Bucket
import gov.cdc.ocio.reportschemavalidator.health.schemaLoadersystem.clientFactory.BlobContainerFactory
import gov.cdc.ocio.reportschemavalidator.health.schemaLoadersystem.clientFactory.S3ClientFactory
import gov.cdc.ocio.reportschemavalidator.utils.AWSS3Configuration
import gov.cdc.ocio.reportschemavalidator.utils.AzureBlobStorageConfiguration
import gov.cdc.ocio.reportschemavalidator.utils.SchemaLoaderSystemType
import io.ktor.server.config.*
import org.koin.dsl.module

object SchemaLoaderModules {

    fun provideS3Module(config: ApplicationConfig, region: String) = module {
        single { SchemaLoaderSystemType.S3 }
        single { AWSS3Configuration(config,configurationPath = "aws") }
        single { S3ClientFactory.createClient(region) }
        single { HealthCheckS3Bucket(get()) }
    }
    fun provideBlobContainerModule(config: ApplicationConfig,connectionString: String, container: String) = module {
        single { SchemaLoaderSystemType.BLOB_STORAGE }
        single { AzureBlobStorageConfiguration(config, configurationPath = "azure") }
        single { BlobContainerFactory.createClient(connectionString , container) }
        single { HealthCheckBlobContainer(get()) }
    }
}

