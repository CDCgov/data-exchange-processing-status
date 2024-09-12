package dextest.plugins

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.statuspages.*
import kotlinx.serialization.Serializable

import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.server.response.*
import java.net.URL
import java.security.interfaces.RSAPublicKey
import org.json.JSONObject

class InvalidTokenException(message: String?) : Exception(message)
class InsufficientScopesException(message: String?) : Exception(message)
class PublicKeyNotFoundException(message: String?) : Exception(message)

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
                } catch (e: Exception) {
                    throw InvalidTokenException(e.message)
                }
            }
        }
    }
    install(StatusPages) {
        exception<InvalidTokenException> { call, cause ->
            call.respond(HttpStatusCode.Forbidden, cause.message ?: "Invalid token")
        }
        exception<InsufficientScopesException> { call, cause ->
            call.respond(HttpStatusCode.Unauthorized, cause.message ?: "Required scopes not found")
        }
        exception<PublicKeyNotFoundException> { call, cause ->
            call.respond(HttpStatusCode.InternalServerError, cause.message ?: "Public key could not be retrieved")
        }
    }
}

fun getIssuerPublicKey(issuer: String, keyId: String): RSAPublicKey? {
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

fun validateJWT(token: String) {
    val issuer = authConfig.issuerUrl

    val decodedJWT = JWT.decode(token)
    val keyId = decodedJWT.keyId

    val publicKey = getIssuerPublicKey(issuer, keyId)

    val algorithm = Algorithm.RSA256(publicKey, null)

    try {
        val verifier = JWT.require(algorithm)
            .withIssuer(issuer)
            .build()

        verifier.verify(decodedJWT)
    } catch (e: Exception) {
        throw InvalidTokenException(e.message)
    }

    val claims = decodedJWT.getClaim("scope").asString() ?: throw IllegalArgumentException("No scopes found")
    val actualScopes = claims.split(" ")

    checkScopes(actualScopes)
}

fun validateOpaqueToken(token: String) {}

fun checkScopes(actualScopes: List<String>) {
    val requiredScopes = authConfig.requiredScopes?.split(" ") ?: return

    if (!requiredScopes.all { it in actualScopes }) {
        throw InsufficientScopesException("One or more required scopes not found")
    }
}
