package gov.cdc.ocio.processingstatusapi.functions

import brave.Tracing
import zipkin2.reporter.AsyncReporter
import zipkin2.reporter.okhttp3.OkHttpSender

object TracingConfig {

//    private const val zipkinEndPoint =
//        "http://tracing-analysis-dc-hz.aliyuncs.com/adapt_aokcdqnxyz@123456ff_abcdef123@abcdef123/api/v2/spans"

    private const val zipkinEndPoint = "http://ocioededevzipkin.eastus.azurecontainer.io:9411/api/v2/spans"

    private const val localServiceName = "dex-processing-status"

    var tracing: Tracing? = null

    init {
        val sender = OkHttpSender.newBuilder().endpoint(zipkinEndPoint).build()

        val reporter = AsyncReporter.builder(sender).build()

        tracing = Tracing.newBuilder().localServiceName(localServiceName).spanReporter(reporter).build()
    }
}
