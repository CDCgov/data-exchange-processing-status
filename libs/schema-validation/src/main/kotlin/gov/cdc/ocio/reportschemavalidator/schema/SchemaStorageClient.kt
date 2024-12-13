package gov.cdc.ocio.reportschemavalidator.schema

import java.io.InputStream

/**
 * This interface defines the contract for all schema storage clients.
 */
interface SchemaStorageClient {
    @Throws(Exception::class)
    fun getSchemaFile(schemaName: String): InputStream
}
