package gov.cdc.ocio.reportschemavalidator.loaders

import gov.cdc.ocio.reportschemavalidator.health.schemaLoadersystem.HealthCheckUnsupportedSchemaLoaderSystem
import gov.cdc.ocio.reportschemavalidator.models.ReportSchemaMetadata
import gov.cdc.ocio.reportschemavalidator.models.SchemaFile
import gov.cdc.ocio.reportschemavalidator.models.SchemaLoaderInfo
import gov.cdc.ocio.types.health.HealthCheckSystem


/**
 * The class which loads the schema files from the class path
 */
class UnsupportedSchemaLoader(
    schemaLoaderName: String?
) : SchemaLoader {

    override fun loadSchemaFile(fileName: String): SchemaFile {
        throw UnsupportedOperationException()
    }

    override fun getSchemaFiles(): List<ReportSchemaMetadata> {
        throw UnsupportedOperationException()
    }

    override fun getInfo(): SchemaLoaderInfo {
        throw UnsupportedOperationException()
    }

    override fun getSchemaContent(schemaFilename: String): Map<String, Any> {
        throw UnsupportedOperationException()
    }

    override fun getSchemaContent(schemaName: String, schemaVersion: String): Map<String, Any> {
        throw UnsupportedOperationException()
    }

    override fun upsertSchema(schemaName: String, schemaVersion: String, content: String): String {
        throw UnsupportedOperationException()
    }

    override fun removeSchema(schemaName: String, schemaVersion: String): String {
        throw UnsupportedOperationException()
    }

    override var healthCheckSystem = HealthCheckUnsupportedSchemaLoaderSystem(
        system,
        schemaLoaderName
    ) as HealthCheckSystem
}
