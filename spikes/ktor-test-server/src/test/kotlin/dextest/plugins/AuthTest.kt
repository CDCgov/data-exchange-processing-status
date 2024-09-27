package dextest.plugins

import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertFailsWith

// Custom exception used in checkScopes
class InsufficientScopesException(message: String) : RuntimeException(message)

// Mock authConfig object
val authConfig = mockk<AuthConfig>()

class AuthTest {

    @Test
    fun `should not throw exception if requiredScopes is empty`() {
        every { authConfig.requiredScopes } returns ""

        checkScopes(emptyList())
    }

    @Test
    fun `should not throw exception if actualScopes contain all requiredScopes`() {
        every { authConfig.requiredScopes } returns "read write"

        val actualScopes = listOf("read", "write", "execute")

        checkScopes(actualScopes)
    }

    @Test
    fun `should throw InsufficientScopesException if actualScopes do not contain all requiredScopes`() {
        every { authConfig.requiredScopes } returns "read write"

        val actualScopes = listOf("read")

        assertFailsWith<InsufficientScopesException> {
            checkScopes(actualScopes)
        }
    }

    @Test
    fun `should not throw exception when requiredScopes is null`() {
        every { authConfig.requiredScopes } returns null

        checkScopes(listOf("read"))
    }

    @Test
    fun `should not throw exception when requiredScopes is blank`() {
        every { authConfig.requiredScopes } returns "   "

        checkScopes(listOf("read"))
    }
}
