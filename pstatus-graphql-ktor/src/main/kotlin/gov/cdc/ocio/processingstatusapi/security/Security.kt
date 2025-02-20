package gov.cdc.ocio.processingstatusapi.security

import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import gov.cdc.ocio.processingstatusapi.exceptions.InsufficientScopesException
import gov.cdc.ocio.processingstatusapi.exceptions.InvalidTokenException
import gov.cdc.ocio.processingstatusapi.exceptions.PublicKeyNotFoundException
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.config.*
import org.json.JSONObject
import java.net.URL
import java.security.interfaces.RSAPublicKey


/**
 * Configure the security for the GraphQL service.
 *
 * @receiver Application
 */
fun Application.configureSecurity(securityEnabled: Boolean) {
    // Get security settings and default to enabled if missing
    // See https://ktor.io/docs/server-jwt.html#configure-verifier
    val issuerUrl = environment.config.tryGetString("security.issuer_url") ?: "http://localhost:9080/realms/test-realm-jwt"
    val introspectionUrl = environment.config.tryGetString("security.introspection_url") ?: "https://some-introspection-url"
    val requiredScopes = environment.config.tryGetString("security.required_scopes") ?: ""

    val authConfig = AuthConfig(
        authEnabled = securityEnabled,
        issuerUrl = issuerUrl,
        introspectionUrl = introspectionUrl,
        requiredScopes = requiredScopes
    )

    if (securityEnabled) {
        install(Authentication) {
            bearer("oauth") {
                authenticate { credentials ->
                    return@authenticate Security(authConfig).authenticate(credentials)
                }
            }
        }
    }
}

/**
 * Security module for the service.
 *
 * @property authConfig AuthConfig
 * @constructor
 */
class Security(private val authConfig: AuthConfig) {

    fun authenticate(credentials: BearerTokenCredential): UserIdPrincipal? {
        if (!authConfig.authEnabled) {
            return UserIdPrincipal("public")
        }

        val token = credentials.token
        try {
            if (token.count { it == '.' } == 2) {
                // Token is JWT, validate using OIDC verifier
                validateJWT(token, authConfig)
            } else {
                // Token is opaque, validate using introspection
                validateOpaqueToken(token)
            }
            UserIdPrincipal("valid-user")
        } catch (e: Exception) {
            throw InvalidTokenException(e.message)
        }

        return null
    }

    /**
     * Validates the provided JWT against the configuration.
     *
     * @param token String
     * @param config AuthConfig
     */
    private fun validateJWT(token: String, config: AuthConfig) {
        val issuer = config.issuerUrl

        val decodedJWT = JWT.decode(token)
        val keyId = decodedJWT.keyId

        val publicKey = getIssuerPublicKey(issuer, keyId)

        val algorithm = Algorithm.RSA256(publicKey, null)

        try {
            val verifier = JWT.require(algorithm).withIssuer(issuer).build()

            verifier.verify(decodedJWT)
        } catch (e: Exception) {
            throw InvalidTokenException(e.message)
        }

        val claims =
            decodedJWT.getClaim("scope").asString() ?: throw InsufficientScopesException("Failed to parse token claims")
        val actualScopes = claims.split(" ")

        checkScopes(actualScopes, config.requiredScopes)
    }

    /**
     * Validates an opaque token.
     *
     * @param token String
     */
    private fun validateOpaqueToken(token: String) {

    }

    /**
     * Gets the issuer public key.
     *
     * @param issuer String
     * @param keyId String?
     * @return RSAPublicKey?
     */
    private fun getIssuerPublicKey(issuer: String, keyId: String?): RSAPublicKey? {
        try {
            val oidcConfigUrl = "$issuer/.well-known/openid-configuration"
            val oidcConfigJson = URL(oidcConfigUrl).readText()

            val jsonObject = JSONObject(oidcConfigJson)
            val jwksUri = jsonObject.getString("jwks_uri")

            val provider = JwkProviderBuilder(URL(jwksUri)).build()
            val jwk = provider.get(keyId)

            return jwk.publicKey as? RSAPublicKey
        } catch (e: Exception) {
            throw PublicKeyNotFoundException("There was an issue retrieving the public key for issuer: $issuer and keyId: $keyId.")
        }
    }

    /**
     * Checks for the required scopes against the actual scopes provided.
     *
     * @param actualScopes List<String>
     * @param requiredScopesList String?
     */
    private fun checkScopes(actualScopes: List<String>, requiredScopesList: String? = "") {
        val requiredScopes = requiredScopesList?.takeIf { it.isNotBlank() }?.split(" ") ?: return

        if (!requiredScopes.all { it in actualScopes }) {
            throw InsufficientScopesException("One or more required scopes not found")
        }
    }

}