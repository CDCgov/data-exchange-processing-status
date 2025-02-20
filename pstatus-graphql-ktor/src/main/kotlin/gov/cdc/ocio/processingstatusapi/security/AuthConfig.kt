package gov.cdc.ocio.processingstatusapi.security


/**
 * AuthN/AuthZ configuration for OAuth2.
 *
 * @property authEnabled Boolean
 * @property issuerUrl String
 * @property introspectionUrl String
 * @property requiredScopes String?
 * @constructor
 */
data class AuthConfig(
    val authEnabled: Boolean,
    val issuerUrl: String,
    val introspectionUrl: String,
    val requiredScopes: String?
)