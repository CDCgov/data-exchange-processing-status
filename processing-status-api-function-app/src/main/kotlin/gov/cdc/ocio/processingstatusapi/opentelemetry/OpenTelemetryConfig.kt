package gov.cdc.ocio.processingstatusapi.opentelemetry

import gov.cdc.ocio.processingstatusapi.functions.traces.TraceUtils
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.exporter.logging.LoggingSpanExporter
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes
import java.util.concurrent.TimeUnit
import io.opentelemetry.api.common.Attributes

/**
 * All SDK management takes place here, away from the instrumentation code, which should only access
 * the OpenTelemetry APIs.
 */
internal object OpenTelemetryConfig1 {
    /**
     * Initializes the OpenTelemetry SDK with a logging span exporter and the W3C Trace Context
     * propagator.
     *
     * @return A ready-to-use [OpenTelemetry] instance.
     */
    fun initOpenTelemetry(): OpenTelemetry {
        val sdkTracerProvider: SdkTracerProvider = SdkTracerProvider.builder()
            .addSpanProcessor(SimpleSpanProcessor.create(LoggingSpanExporter()))
            .build()
        val sdk: OpenTelemetrySdk = OpenTelemetrySdk.builder()
            .setTracerProvider(sdkTracerProvider)
            .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
            .build()
        Runtime.getRuntime().addShutdownHook(Thread(sdkTracerProvider::close))
        return sdk
    }
}

internal object OpenTelemetryConfig {
    /**
     * Initialize an OpenTelemetry SDK with a [OtlpGrpcSpanExporter] and a [ ].
     *
     * @param jaegerEndpoint The endpoint of your Jaeger instance.
     * @return A ready-to-use [OpenTelemetry] instance.
     */
    fun initOpenTelemetry(): OpenTelemetry? {
        if (!TraceUtils.tracingEnabled)
            return null

        // Export traces to Jaeger over OTLP
        val jaegerOtlpExporter: OtlpGrpcSpanExporter = OtlpGrpcSpanExporter.builder()
            .setEndpoint(System.getenv("JAEGER_OTEL_COLLECTOR_END_POINT"))
            .setTimeout(30, TimeUnit.SECONDS)
            .build()
        val serviceNameResource: Resource =
            Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, "dex-processing-status"))

        // Set to process the spans by the Jaeger Exporter
        val tracerProvider = SdkTracerProvider.builder()
            .addSpanProcessor(BatchSpanProcessor.builder(jaegerOtlpExporter).build())
            .setResource(Resource.getDefault().merge(serviceNameResource))
            .build()
        val openTelemetry = OpenTelemetrySdk.builder().setTracerProvider(tracerProvider).build()

        // it's always a good idea to shut down the SDK cleanly at JVM exit.
        Runtime.getRuntime().addShutdownHook(Thread { tracerProvider.close() })
        return openTelemetry
    }
}