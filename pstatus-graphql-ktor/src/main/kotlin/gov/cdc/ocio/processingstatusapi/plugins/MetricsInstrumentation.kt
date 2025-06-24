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
     * @param parameters the parameters containing the GraphQL request document and associated information, or null if unavailable.
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
            override fun onDispatched() {}
            override fun onCompleted(result: List<ValidationError>?, t: Throwable?) {}
        }
    }
}
