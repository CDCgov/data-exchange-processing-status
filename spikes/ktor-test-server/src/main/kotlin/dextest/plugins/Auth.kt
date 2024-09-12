package dextest.plugins

import io.ktor.server.application.*
import io.ktor.server.auth.*
import kotlinx.serialization.Serializable

import com.google.auth.oauth2.TokenVerifier

import com.nimbusds.jwt.SignedJWT
import com.nimbusds.jose.crypto.RSASSAVerifier
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import org.json.JSONObject
import java.net.URL
import java.security.interfaces.RSAPublicKey


@Serializable
data class Claims(val scope: String)

@Serializable
data class IntrospectionResponse(val active: Boolean, val scope: String)

data class AuthConfig(
    val issuerUrl: String, val introspectionUrl: String, val requiredScopes: String?
)

// Dummy appConfig, replace with actual config loading logic
val authConfig = AuthConfig(
    issuerUrl = "https://apigw-stg.cdc.gov:8443",
    introspectionUrl = "https://your-introspection-url",
    requiredScopes = "dex:upload"
)

fun Application.configureAuth() {
    install(Authentication) {
        bearer("oauth") {
            authenticate { credentials ->
                val token = credentials.token
                try {
                    if (token.count { it == '.' } == 2) {
                        // Token is JWT, validate using OIDC verifier
                        validateJWT(token)
                    } else {
                        // Token is opaque, validate using introspection
                        validateOpaqueToken(token)
                    }
                    UserIdPrincipal("valid-user")
                } catch (e: IllegalArgumentException) {
                    // Invalid token format or scope issue, respond with unauthorized
                    null
                }
            }
        }
    }
}

suspend fun getIssuerPublicKey(issuer: String, keyId: String): RSAPublicKey? {
    // Fetch the OIDC configuration to get the JWKS URI
    val oidcConfigUrl = "$issuer/.well-known/openid-configuration"
    val oidcConfigJson = URL(oidcConfigUrl).readText()

    // Parse the JSON string
    val jsonObject = JSONObject(oidcConfigJson)

    // Get the JWKS URI from the JSON
    val jwksUri = jsonObject.getString("jwks_uri")

    // Fetch the JWKS from the JWKS URI
    val jwksJson = URL(jwksUri).readText()

    // Parse the JWKS
    val jwkSet = JWKSet.parse(jwksJson)

    // Find the JWK with the matching key ID
    val jwk: JWK? = jwkSet.keys.find { it.keyID == keyId }

    // Convert the JWK to an RSA public key
    return (jwk as? RSAKey)?.toRSAPublicKey()
}

suspend fun validateJWT(token: String) {
    val issuer = authConfig.issuerUrl

    // Parse the JWT
    val signedJWT = SignedJWT.parse(token)

    // Get the Key ID from the JWT header
    val keyId = signedJWT.header.keyID

    // Get the public key for the issuer using the Key ID
    val publicKey = getIssuerPublicKey(issuer, keyId)
        ?: throw IllegalArgumentException("Public key not found for Key ID: $keyId")

    // Create the JWS verifier
    val verifier = RSASSAVerifier(publicKey)

    // Verify the token's signature
    if (!signedJWT.verify(verifier)) {
        throw IllegalArgumentException("Invalid JWT token")
    }

    val claims = signedJWT.jwtClaimsSet.getStringClaim("scope") ?: throw IllegalArgumentException("No scopes found")
    val actualScopes = claims.split(" ")

    checkScopes(actualScopes)

//    using Google Oauth2 package
//    val verifier = TokenVerifier.newBuilder().setIssuer(authConfig.issuerUrl).setPublicKey(publicKey).build()
//
//    val idToken = verifier.verify(token) ?: throw IllegalArgumentException("Invalid JWT token")
//
//    val claims: Claims = idToken.claims["scope"] ?: throw IllegalArgumentException("No scopes found")
//    val actualScopes = claims.split(" ")
//
//    checkScopes(actualScopes)
}

suspend fun validateOpaqueToken(token: String) {}

fun checkScopes(actualScopes: List<String>) {
    val requiredScopes = authConfig.requiredScopes?.split(" ") ?: return

    if (!requiredScopes.all { it in actualScopes }) {
        throw IllegalArgumentException("One or more required scopes not found")
    }
}
