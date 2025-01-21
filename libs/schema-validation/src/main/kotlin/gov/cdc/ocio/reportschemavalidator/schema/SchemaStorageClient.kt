package gov.cdc.ocio.reportschemavalidator.schema

import gov.cdc.ocio.reportschemavalidator.models.ReportSchemaMetadata
import gov.cdc.ocio.reportschemavalidator.models.SchemaLoaderInfo
import gov.cdc.ocio.types.health.HealthCheckSystem


/**
 * This interface defines the contract for all schema storage clients.
 */
interface SchemaStorageClient {
    @Throws(Exception::class)
    fun getSchemaFile(fileName: String): String

    fun getSchemaFiles(): List<ReportSchemaMetadata>

    fun getInfo(): SchemaLoaderInfo

    fun getSchemaContent(schemaFilename: String): Map<String, Any>

    fun getSchemaContent(schemaName: String, schemaVersion: String): Map<String, Any>

    var healthCheckSystem: HealthCheckSystem
}
