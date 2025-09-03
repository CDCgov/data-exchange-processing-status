package gov.cdc.ocio.processingstatusapi.plugins

import graphql.execution.instrumentation.*
import graphql.execution.instrumentation.parameters.InstrumentationValidationParameters
import graphql.language.OperationDefinition
import graphql.validation.ValidationError
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.metrics.LongCounter
import graphql.language.Field


/**
 * MetricsInstrumentation is a custom GraphQL instrumentation implementation that tracks and reports
 * metrics for GraphQL operations using OpenTelemetry.
 *
 * This class extends from SimplePerformantInstrumentation and overrides specific methods to add metrics
 * counters for operations processed within the GraphQL server.
 *
 * @constructor Creates an instance of MetricsInstrumentation.
 *
 * @param graphqlOperationCounter A LongCounter object used to count the number of operations performed
 * in the GraphQL API. The counter tracks both the types of operations (query, mutation, etc.)
 * and the operation names.
 */
class MetricsInstrumentation(
    private val graphqlOperationCounter: LongCounter
) : SimplePerformantInstrumentation() {

    /**
     * Begins the validation process for a GraphQL instrumentation, processes the provided operation definition,
     * updates relevant metrics, and returns a no-op instrumentation context for validation errors.
     *
     * @param parameters the parameters containing the GraphQL request document and associated information, or null
     * if unavailable.
     * @param state the state shared across different phases of instrumentation, or null if not provided.
     * @return an instance of InstrumentationContext that tracks the validation process for a list of validation errors.
     */
    override fun beginValidation(
        parameters: InstrumentationValidationParameters?,
        state: InstrumentationState?
    ): InstrumentationContext<List<ValidationError>> {
        val document = parameters?.document

        // Get the first operation definition (GraphQL allows multiple, but you usually only have one)
        val operationDefinition = document?.definitions
            ?.filterIsInstance<OperationDefinition>()
            ?.firstOrNull()

        val operationName = operationDefinition?.selectionSet?.selections
                ?.filterIsInstance<Field>()
                ?.firstOrNull()
                ?.name
            ?: "Unknown"

        val operationType = operationDefinition?.operation?.name ?: "Unknown"

        graphqlOperationCounter.add(
            1,
            Attributes.of(
                AttributeKey.stringKey("operation"), operationName,
                AttributeKey.stringKey("operation_type"), operationType
            )
        )

        // Return a no-op context
        return object : InstrumentationContext<List<ValidationError>> {
            /**
             * Handles the dispatching phase of the instrumentation process.
             *
             * This method is invoked when an operation or event reaches the dispatched
             * state within the GraphQL instrumentation pipeline. Typically, this method
             * can be used to perform actions or update metrics right after an operation
             * is dispatched, without waiting for its completion.
             *
             * The default implementation is empty.
             */
            override fun onDispatched() { /* no-op */ }

            /**
             * Invoked when the process involving the list of validation errors is completed. This method handles
             * the post-completion behavior within the GraphQL instrumentation pipeline. It can be used to perform
             * logging, error handling, or updating relevant metrics after the validation process concludes.
             *
             * @param result an optional list of validation errors generated during the validation process, or null if
             * no errors occurred.
             * @param t an optional throwable representing any exception that might have occurred during the process,
             * or null if no exception occurred.
             */
            override fun onCompleted(result: List<ValidationError>?, t: Throwable?) { /* no-op */ }
        }
    }
}
