package gov.cdc.ocio.processingstatusapi.security

/**
 * Defines the schema security configuration used to secure the schema administration mutations to upsert or replace
 * schemas.
 *
 * @property token String? - Secret token used for authorization
 * @constructor
 */
data class SchemaSecurityConfig(val token: String?)