package gov.cdc.ocio.processingstatusapi.plugins

import com.expediagroup.graphql.generator.extensions.plus
import com.expediagroup.graphql.server.ktor.DefaultKtorGraphQLContextFactory
import graphql.GraphQLContext
import io.ktor.server.request.*
import io.ktor.util.*

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
    override suspend fun generateContext(request: ApplicationRequest): GraphQLContext =
        super.generateContext(request).plus(
            // Add the AuthContext to the graphql context
            mapOf("AuthContext" to (request.call.attributes.getOrNull(AttributeKey("AuthContext"))))
        )
}