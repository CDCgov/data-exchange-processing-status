package dextest.plugins

import kotlin.test.Test
import kotlin.test.assertFailsWith

// Custom exception used in checkScopes
class InsufficientScopesException(message: String) : RuntimeException(message)

class AuthTest {

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
