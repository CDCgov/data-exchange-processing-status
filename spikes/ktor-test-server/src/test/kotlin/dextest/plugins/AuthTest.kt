package dextest.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.interfaces.DecodedJWT
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertFailsWith

class AuthTest {
    private val mockConfig = AuthConfig(
        authEnabled = true,
        issuerUrl = "issuer.com",
        introspectionUrl = "",
        requiredScopes = "read write"
    )

    @BeforeTest
    fun setup() {
        mockkStatic(JWT::class)
        mockkStatic(::getIssuerPublicKey)
    }

    @AfterTest
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `should throw InvalidTokenException for invalid JWT`() {
        val invalidToken = "invalid.jwt.token"

        every { JWT.decode(invalidToken) } throws InvalidTokenException("Invalid Token")

        assertFailsWith<InvalidTokenException> {
            validateJWT(invalidToken, mockConfig)
        }
    }

    @Ignore
    @Test
    fun `should throw PublicKeyNotFound exception when getIssuerPublicKey fails`() {
        val validToken = "valid.jwt.token"
        val mockDecodedJWT = mockk<DecodedJWT>()
        val mockKeyId = "keyId"

        every { JWT.decode(validToken) } returns mockDecodedJWT
        every { mockDecodedJWT.keyId } returns mockKeyId
        // TODO: figure out why this function is not being mocked properly
        every {
            getIssuerPublicKey(any(), any())
        } throws PublicKeyNotFoundException("Public key not found")

        assertFailsWith<PublicKeyNotFoundException> {
            validateJWT(validToken, mockConfig)
        }
    }

    // Tests for checkScopes function
    @Test
    fun `should not throw exception if requiredScopes is empty`() {
        checkScopes(emptyList())
    }

    @Test
    fun `should not throw exception if actualScopes contain all requiredScopes`() {
        val requiredScopeList = "read write execute"
        val actualScopes = listOf("read", "write", "execute")

        checkScopes(actualScopes, requiredScopeList)
    }

    @Test
    fun `should throw InsufficientScopesException if actualScopes do not contain all requiredScopes`() {
        val requiredScopeList = "read write"
        val actualScopes = listOf("read")

        assertFailsWith<InsufficientScopesException> {
            checkScopes(actualScopes, requiredScopeList)
        }
    }
}
