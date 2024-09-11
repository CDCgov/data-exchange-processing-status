package dextest.plugins

import io.ktor.server.application.*
import io.ktor.server.auth.*
import kotlinx.serialization.Serializable

@Serializable
data class Claims(val scope: String)

@Serializable
data class IntrospectionResponse(val active: Boolean, val scope: String)

@Serializable
data class OIDCConfiguration(
    val issuer: String, val jwks_uri: String
)

data class AuthConfig(
    val issuerUrl: String, val introspectionUrl: String, val requiredScopes: String?
)

// Dummy appConfig, replace with actual config loading logic
val authConfig = AuthConfig(
    issuerUrl = "https://your-issuer-url",
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
    throw IllegalArgumentException("Not valid JWT")
//    val issuer = authConfig.issuerUrl
//
//    // Step 1: Fetch JWKS from OIDC discovery document
//    val jwksUrl = fetchJWKSUrl(issuer)
//
//    // Step 2: Retrieve the keys from the JWKS
//    val publicKey = getPublicKeyFromJWKS(jwksUrl)
//
//    // Step 3: Validate and parse the JWT
//    try {
//        val jwtParser = Jwts.parserBuilder()
//            .setSigningKey(publicKey) // Use the public key for signature verification
//            .deserializeJsonWith(JacksonDeserializer(mapOf("alg" to "RS256")))
//            .requireIssuer(issuer)
//            .build()
//
//        val claims = jwtParser.parseClaimsJws(token)
//        println("JWT is valid. Claims: ${claims.body}")
//
//        // Add custom scope checking logic here
//        // Example: val scopes = claims.body["scope"].toString().split(" ")
//
//        return true
//    } catch (e: JwtException) {
//        println("Invalid JWT: ${e.message}")
//        return false
//    }
}

// Helper function to get JWKS URL from OIDC
//fun fetchJWKSUrl(issuer: String): String = runBlocking {
//    val client = HttpClient(CIO)
//    val discoveryDocumentUrl = "$issuer/.well-known/openid-configuration"
//
//    val res = client.get(discoveryDocumentUrl)
//    println("JWks response: $res")
//    val config = res.body()
//    client.close()
//    return@runBlocking config["jwks_uri"].toString()
//}

// Helper function to retrieve the public key from JWKS
//fun getPublicKeyFromJWKS(jwksUrl: String): PublicKey = runBlocking {
//    val client = HttpClient(CIO)
//    val jwks: Map<String, Any> = client.get(jwksUrl)
//    client.close()
//
//    // Assuming a single key in the JWKS
//    val keys = jwks["keys"] as List<Map<String, Any>>
//    val keyData = keys.first()
//
//    val x5c = keyData["x5c"] as List<String>
//    val decodedKey = Base64.getDecoder().decode(x5c.first())
//    val keySpec = X509EncodedKeySpec(decodedKey)
//
//    val keyFactory = KeyFactory.getInstance("RSA")
//    return@runBlocking keyFactory.generatePublic(keySpec)
//}

suspend fun validateOpaqueToken(token: String) {}

fun checkScopes(actualScopes: List<String>) {
    val requiredScopes = authConfig.requiredScopes?.split(" ") ?: return

    if (!requiredScopes.all { it in actualScopes }) {
        throw IllegalArgumentException("One or more required scopes not found")
    }
}
