package dextest.plugins

import io.ktor.server.application.*
import io.ktor.server.auth.*
import kotlinx.serialization.Serializable

import com.google.auth.oauth2.TokenVerifier


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
    requiredScopes = "read write"
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

fun validateJWT(token: String) {
    val verifier = TokenVerifier.newBuilder().setIssuer(authConfig.issuerUrl).build()

    val idToken = verifier.verify(token) ?: throw IllegalArgumentException("Invalid JWT token")

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
