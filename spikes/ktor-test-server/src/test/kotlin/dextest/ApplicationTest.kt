package dextest

import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.JWT
import dextest.plugins.authConfig
import dextest.plugins.configureAuth
import dextest.plugins.configureRouting
import io.ktor.client.request.header
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import java.math.BigInteger
import java.net.ServerSocket
import java.net.URL
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.time.Instant
import java.util.Base64
import java.util.Date
import kotlin.test.*
import kotlin.test.assertEquals
import kotlin.test.fail
import kotlin.test.Test
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Assert.assertNotNull


// Global for the RSA key pair
val keyId = "test-key-id"
var keyPair = generateRSAKeyPair()
val publicKey = keyPair.public as RSAPublicKey // access the public key

var MOCK_OIDC_PORT = findFreeListenPort()
var testCaseMockOidcBaseUrl = "http://localhost:${MOCK_OIDC_PORT}"
var testCaseIssuerUrl = testCaseMockOidcBaseUrl

// Mock tokens
val mockTokenValid = createMockJWT(testCaseIssuerUrl, 1, "")

// create EXPIRED mock token w/ -1-hour expire offset
val mockTokenExpired = createMockJWT(testCaseIssuerUrl, -1, "")

// create mock token by concat a Z to make an invalid signature
val mockTokenInvalidSignature = mockTokenValid + "Z"

// create token with wrong issuer
val mockTokenWrongIssuer = createMockJWT("http://wrong-issuer.com", 1, "")

// create VALID mock token w/ +1-hour expire offset with scope
val mockTokenValidWithScope = createMockJWT(testCaseIssuerUrl, 1, "testscope1 testscope2")

// setup up VALID mock token w/ +1-hour expire offset that includes the scopes
val mockTokenValidIncludesReqScopes = createMockJWT(testCaseIssuerUrl, 1, "read:scope1 read:custom1 write:scope1 write:custom1")


// Test case for testing OAuth auth middleware
data class TestCase(
    val name: String,
    val issuerURL: String,
    val authEnabled: Boolean,
    val authHeader: String,
    val expectStatus: Int,
    val expectMesg: String,
    val requiredScopes: String,
)

// List of test cases
val testCases =
    listOf(
        TestCase(
            name = "Missing Authorization Header",
            issuerURL = testCaseIssuerUrl,
            authEnabled = true,
            authHeader = "",
            expectStatus = HttpStatusCode.Unauthorized.value,
            expectMesg = "Unauthorized you need to provide valid credentials",
            requiredScopes = "",
        ),
        TestCase(
            name = "Empty Auth Header Token",
            issuerURL = testCaseIssuerUrl,
            authEnabled = true,
            authHeader = "Bearer",
            expectStatus = HttpStatusCode.Unauthorized.value,
            expectMesg = "Unauthorized you need to provide valid credentials",
            requiredScopes = "",
        ),
        TestCase(
            name = "Invalid JSON Auth Header Format",
            issuerURL = testCaseIssuerUrl,
            authEnabled = true,
            authHeader = "Bearer invalid.jwt.token",
            expectStatus = HttpStatusCode.Forbidden.value,
            expectMesg = "doesn't have a valid JSON format",
            requiredScopes = "",
        ),
        TestCase(
            name = "Invalid EMPTY Auth Header Format",
            issuerURL = testCaseIssuerUrl,
            authEnabled = true,
            authHeader = "Bearer",
            expectStatus = HttpStatusCode.Unauthorized.value,
            expectMesg = "Unauthorized you need to provide valid credentials",
            requiredScopes = "",
        ),
        TestCase(
            name = "Expired JWT Token",
            issuerURL = testCaseIssuerUrl,
            authEnabled = true,
            authHeader = "Bearer " + mockTokenExpired,
            expectStatus = HttpStatusCode.Forbidden.value,
            expectMesg = "The Token has expired on",
            requiredScopes = "",
        ),
        TestCase(
            name = "Invalid JWT Signature",
            issuerURL = testCaseIssuerUrl,
            authEnabled = true,
            authHeader = "Bearer " + mockTokenInvalidSignature,
            expectStatus = HttpStatusCode.Forbidden.value,
            expectMesg = "Token's Signature resulted invalid when verified using the Algorithm",
            requiredScopes = "",
        ),
        TestCase(
            name = "Invalid Issuer",
            issuerURL = testCaseIssuerUrl,
            authEnabled = true,
            authHeader = "Bearer " + mockTokenWrongIssuer,
            expectStatus = HttpStatusCode.Forbidden.value,
            expectMesg = "value doesn't match the required issuer",
            requiredScopes = "",
        ),
        TestCase(
            name = "Valid Auth Token with empty Scopes Server Does Not Require",
            issuerURL = testCaseIssuerUrl,
            authEnabled = true,
            authHeader = "Bearer " + mockTokenValid,
            expectStatus = HttpStatusCode.OK.value,
            expectMesg = "You have reached a protected route, valid-user",
            requiredScopes = "",
        ),
        TestCase(
            name = "Valid JWT Has Scope Claim Server Does Not Require",
            issuerURL = testCaseIssuerUrl,
            authEnabled = true,
            authHeader = "Bearer " + mockTokenValidWithScope,
            expectStatus = HttpStatusCode.OK.value,
            expectMesg = "You have reached a protected route, valid-user",
            requiredScopes = "",
        ),
        TestCase(
            name = "Valid JWT bad issuerURL",
            issuerURL = "http://bad-issuer.com",
            authEnabled = true,
            authHeader = "Bearer " + mockTokenValidWithScope,
            expectStatus = HttpStatusCode.Forbidden.value,
            expectMesg = "There was an issue retrieving the public key for issuer",
            requiredScopes = "",
        ),
        // RequiredScopes related tests
        TestCase(
            name = "JWT Has No Scope Claim Server Does Require Scopes",
            issuerURL = testCaseIssuerUrl,
            authEnabled = true,
            authHeader = "Bearer " + mockTokenValid,
            expectStatus = HttpStatusCode.Forbidden.value,
            expectMesg = "One or more required scopes not found",
            requiredScopes = "read:scope1",
        ),
        TestCase(
            name = "JWT Has Custom and One Scope and One Missing Claim Server Does Require Scopes",
            issuerURL = testCaseIssuerUrl,
            authEnabled = true,
            authHeader = "Bearer " + mockTokenValidIncludesReqScopes,
            expectStatus = HttpStatusCode.Forbidden.value,
            expectMesg = "One or more required scopes not found",
            requiredScopes = "read:scope1 write:scope1 read:scope2",
        ),
        TestCase(
            name = "Valid JWT Token includes custom and both required scopes",
            issuerURL = testCaseIssuerUrl,
            authEnabled = true,
            authHeader = "Bearer " + mockTokenValidIncludesReqScopes,
            expectStatus = HttpStatusCode.OK.value,
            expectMesg = "You have reached a protected route, valid-user",
            requiredScopes = "read:scope1 write:scope1",
        ),
    )

class ApplicationTest {
    @Test
    fun testKeyPairIsInitialized() {
        // Test that global keyPair is setup
        assertNotNull(keyPair.private, "Private key not set") 
        assertNotNull(keyPair.public, "Public key not set")
    }

    @Test
    fun testRoot() = testApplication {
        application {
            configureAuth()
            configureRouting()
        }
        client.get("/").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("Hello World!", bodyAsText())
        }
    }

    @Test
    fun testProtectedAuthEnabled() = testApplication {
        mockkObject(authConfig)
        every { authConfig.authEnabled } returns true

        application {
            configureAuth()
            configureRouting()
        }

        client.get("/protected").apply {
            assertEquals(HttpStatusCode.Unauthorized, status)
            assertEquals("Unauthorized you need to provide valid credentials", bodyAsText())
        }
    }

    @Test
    fun testProtectedAuthDisabled() = testApplication {
        mockkObject(authConfig)
        every { authConfig.authEnabled } returns false 

        application {
            configureAuth()
            configureRouting()
        }

        client.get("/protected").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("You have reached a protected route, null", bodyAsText())
        }
    }

    // Iterate through the auth middleware test cases
    @Test
    fun runAuthTestCases() = testApplication {
        application {
            configureAuth()
            configureRouting()
        }

        // Loop over each test case
        testCases.forEach { testCase ->
            println("RUNNING TEST CASE: ${testCase.name}")

            // Setup mock server for OIDC endpoints
            val mockOidcServer = MockWebServer()
            mockOidcServer.start(MOCK_OIDC_PORT)

            // Base URL for the OIDC MockWebServer
            val baseUrl = mockOidcServer.url("/").toString().removeSuffix("/")

            val oidcConfigResponse =
                """
                {
                    "issuer": "$baseUrl",
                    "authorization_endpoint": "$baseUrl/oauth2/authorize",
                    "token_endpoint": "$baseUrl/oauth2/token",
                    "jwks_uri": "$baseUrl/oauth2/jwks"
                }
                """.trimIndent()

            val modulus = Base64.getUrlEncoder().withoutPadding().encodeToString(publicKey.modulus.toByteArray())
            val exponent =
                Base64.getUrlEncoder().withoutPadding().encodeToString(
                    BigInteger.valueOf(publicKey.publicExponent.toLong()).toByteArray(),
                )

            val oidcJwksResponse =
                """
                    {
                      "keys": [
                        {
                          "kty": "RSA",
                          "kid": "test-key-id",
                          "n": "$modulus",
                          "e": "$exponent"
                }
                      ]
                    }
                """.trimIndent()

            mockOidcServer.dispatcher =
                object : Dispatcher() {
                    override fun dispatch(request: RecordedRequest): MockResponse =
                        when (request.path) {
                            "/.well-known/openid-configuration" -> MockResponse().setResponseCode(HttpStatusCode.OK.value).setBody(oidcConfigResponse)
                            "/oauth2/jwks" -> MockResponse().setResponseCode(HttpStatusCode.OK.value).setBody(oidcJwksResponse)
                            else -> MockResponse().setResponseCode(HttpStatusCode.NotFound.value).setBody("Not Found")
                        }
                }

            mockkObject(authConfig)

            every { authConfig.authEnabled } returns testCase.authEnabled
            every { authConfig.issuerUrl } returns testCase.issuerURL
            every { authConfig.introspectionUrl } returns "http://mock-introspection-url"
            every { authConfig.requiredScopes } returns testCase.requiredScopes

            // Perform the GET request for the auth protected path
            val response =
                client.get("/protected") {
                    if (testCase.authHeader.isNotEmpty()) {
                        header("Authorization", testCase.authHeader)
                    }
                }

            // Assert the status code
            assertEquals(testCase.expectStatus, response.status.value, "Expected status code for ${testCase.name}")

            // Assert the response message contains the expected message
            if (testCase.expectMesg != "") {
                assertTrue(
                    response.bodyAsText().contains(testCase.expectMesg),
                    "Expected response message to contain '${testCase.expectMesg}' for ${testCase.name}, but got '${response.bodyAsText()}'",
                )
            }

            // Clean up for next test case.
            unmockkAll()
            mockOidcServer.shutdown()
        }
    }
}

fun createMockJWT(
    issuerUrl: String,
    expireOffsetHours: Long,
    scopes: String = "",
): String {
    // Set expiration time based on the offset
    val expirationTime = Date.from(Instant.now().plusSeconds(expireOffsetHours * 3600))

    // Create JWT claims
    val jwtBuilder =
        JWT
            .create()
            .withSubject("1234567890")
            .withClaim("name", "John Doe")
            .withIssuedAt(Date())
            .withIssuer(issuerUrl)
            .withExpiresAt(expirationTime)
            .withKeyId(keyId)
            .withClaim("scope", scopes)

    // Sign the token using RS256 and the private key
    return jwtBuilder
        .sign(Algorithm.RSA256(null, keyPair.private as RSAPrivateKey))
}

// Helper function to generate an RSA key pair
fun generateRSAKeyPair(): KeyPair {
    val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
    keyPairGenerator.initialize(2048)
    return keyPairGenerator.generateKeyPair()
}

fun findFreeListenPort(): Int {
    ServerSocket(0).use { socket ->
        return socket.localPort
    }
}