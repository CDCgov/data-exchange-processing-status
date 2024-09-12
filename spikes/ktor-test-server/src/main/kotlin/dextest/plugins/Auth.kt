package dextest.plugins

import io.ktor.server.application.*
import io.ktor.server.auth.*
import kotlinx.serialization.Serializable

import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.net.URL
import java.security.interfaces.RSAPublicKey
import org.json.JSONObject


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

fun getIssuerPublicKey(issuer: String, keyId: String): RSAPublicKey? {

    val oidcConfigUrl = "$issuer/.well-known/openid-configuration"
    val oidcConfigJson = URL(oidcConfigUrl).readText()

    val jsonObject = JSONObject(oidcConfigJson)
    val jwksUri = jsonObject.getString("jwks_uri")

    val provider = JwkProviderBuilder(URL(jwksUri)).build()
    val jwk = provider.get(keyId)

    return jwk.publicKey as? RSAPublicKey
}

fun validateJWT(token: String) {
    val issuer = authConfig.issuerUrl

    val decodedJWT = JWT.decode(token)
    val keyId = decodedJWT.keyId

    val publicKey = getIssuerPublicKey(issuer, keyId)
        ?: throw IllegalArgumentException("Public key not found for Key ID: $keyId")

    val algorithm = Algorithm.RSA256(publicKey, null)

    val verifier = com.auth0.jwt.JWT.require(algorithm)
        .withIssuer(issuer)
        .build()

    verifier.verify(decodedJWT)

    val claims = decodedJWT.getClaim("scope").asString() ?: throw IllegalArgumentException("No scopes found")
    val actualScopes = claims.split(" ")

    checkScopes(actualScopes)
}

fun validateOpaqueToken(token: String) {}

fun checkScopes(actualScopes: List<String>) {
    val requiredScopes = authConfig.requiredScopes?.split(" ") ?: return

    if (!requiredScopes.all { it in actualScopes }) {
        throw IllegalArgumentException("One or more required scopes not found")
    }
}
