package gov.cdc.ocio.processingstatusapi.plugins

import gov.cdc.ocio.processingstatusapi.exceptions.ForbiddenException
import gov.cdc.ocio.processingstatusapi.exceptions.InsufficientScopesException
import gov.cdc.ocio.processingstatusapi.exceptions.InvalidTokenException
import gov.cdc.ocio.processingstatusapi.exceptions.PublicKeyNotFoundException
import graphql.GraphqlErrorBuilder
import graphql.execution.DataFetcherExceptionHandler
import graphql.execution.DataFetcherExceptionHandlerParameters
import graphql.execution.DataFetcherExceptionHandlerResult
import mu.KotlinLogging
import java.util.concurrent.CompletableFuture


/**
 * Custom GraphQL exception handler.  Note that install(StatusPages) pattern normally used for this in ktor does not
 * work with GraphQL as GraphQL should always return a 200.  This custom exception handler allows us to provide more
 * context around the exception that has been thrown.
 *
 * @property logger KLogger
 */
class CustomGraphQLExceptionHandler : DataFetcherExceptionHandler {
    private val logger = KotlinLogging.logger {}

    override fun handleException(
        params: DataFetcherExceptionHandlerParameters
    ): CompletableFuture<DataFetcherExceptionHandlerResult> {

        val exception = params.exception
        logger.error("GraphQL Error: ${exception.message}", exception)

        val classification = when (exception) {
            is UnsupportedOperationException -> "UnsupportedOperationException"
            is ForbiddenException -> "ForbiddenException"
            is InvalidTokenException -> "InvalidTokenException"
            is InsufficientScopesException -> "InsufficientScopesException"
            is PublicKeyNotFoundException -> "PublicKeyNotFoundException"
            else -> "InternalServerError"
        }

        return CompletableFuture.supplyAsync {
            DataFetcherExceptionHandlerResult.newResult()
                .error(
                    GraphqlErrorBuilder.newError()
                        .message(exception.message ?: "An error occurred")
                        .extensions(mapOf("classification" to classification))
                        .path(params.path)
                        .build()
                )
                .build()
        }
    }
}