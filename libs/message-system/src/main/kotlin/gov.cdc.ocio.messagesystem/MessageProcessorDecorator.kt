package gov.cdc.ocio.messagesystem

import io.opentelemetry.api.GlobalOpenTelemetry
import kotlin.time.measureTimedValue

/**
 * This class is a decorator for a `MessageProcessorInterface` implementation. It extends the functionality
 * of the delegate message processor by recording and reporting the duration of message processing using
 * an OpenTelemetry-based histogram metric.
 *
 * @constructor Creates an instance of the `MessageProcessorDecorator`.
 * @param instrumentScopeName The name of the OpenTelemetry instrument scope used for metric tracking.
 * @param delegate The `MessageProcessorInterface` instance to which the message processing task is delegated.
 */
class MessageProcessorDecorator(
    instrumentScopeName: String,
    private val delegate: MessageProcessorInterface
) : MessageProcessorInterface {

    private val meter = GlobalOpenTelemetry.get().getMeter(instrumentScopeName)

    private val processMessageHistogram = meter
        .histogramBuilder("process_message_duration")
        .setDescription("Time taken to process a message")
        .setUnit("ms")
        .ofLongs()
        .build()

    /**
     * Processes the provided message while also measuring the time taken to complete the operation.
     * The duration of message processing is recorded in a histogram metric for monitoring purposes.
     *
     * @param message The message to be processed, represented as a string.
     */
    override fun processMessage(message: String) {
        val (result, duration) = measureTimedValue {
            delegate.processMessage(message)
        }
        processMessageHistogram.record(duration.inWholeMilliseconds)
        return result
    }
}