package gov.cdc.ocio.reportschemavalidator.schema

import gov.cdc.ocio.reportschemavalidator.models.ReportSchemaMetadata
import gov.cdc.ocio.reportschemavalidator.models.SchemaLoaderInfo
import java.io.InputStream

/**
 * This interface defines the contract for all schema storage clients.
 */
interface SchemaStorageClient {
    @Throws(Exception::class)
    fun getSchemaFile(schemaName: String): InputStream

    fun getSchemaFiles(): List<ReportSchemaMetadata>

    fun getInfo(): SchemaLoaderInfo
}
