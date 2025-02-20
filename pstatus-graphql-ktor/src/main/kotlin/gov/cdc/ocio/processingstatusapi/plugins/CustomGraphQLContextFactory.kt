package gov.cdc.ocio.processingstatusapi.plugins

import com.expediagroup.graphql.generator.extensions.plus
import com.expediagroup.graphql.server.ktor.DefaultKtorGraphQLContextFactory
import graphql.GraphQLContext
import io.ktor.server.request.*
import io.ktor.util.*


data class AuthContext(val token: String?)


/**
 * Custom graphql context so that we have include additional context to the default graphql context.
 */
class CustomGraphQLContextFactory : DefaultKtorGraphQLContextFactory() {

    /**
     * Override the method to generate context so we can add to it.
     *
     * @param request ApplicationRequest
     * @return GraphQLContext
     */
    override suspend fun generateContext(request: ApplicationRequest): GraphQLContext {
        val context = super.generateContext(request)

        val map = mutableMapOf<String, Any>()
        // Add the AuthContext to the graphql context
        val authContext = request.call.attributes.getOrNull(AttributeKey("AuthContext"))
        authContext?.let { map.put("AuthContext", authContext) }
        val headers = request.headers["Authorization"]
        map["Authorization"] = AuthContext(headers)
        context.plus(map)

        return context
    }
}