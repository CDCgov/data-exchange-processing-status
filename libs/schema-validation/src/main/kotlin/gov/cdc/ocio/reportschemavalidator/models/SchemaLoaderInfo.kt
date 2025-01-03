package gov.cdc.ocio.reportschemavalidator.models


/**
 * Defines the schema loader and schema files location.
 *
 * @property type String
 * @property location String
 * @constructor
 */
data class SchemaLoaderInfo(

    val type: String, // e.g. s3, blob storage, file

    val location: String // e.g. fully qualified folder or container name
)
