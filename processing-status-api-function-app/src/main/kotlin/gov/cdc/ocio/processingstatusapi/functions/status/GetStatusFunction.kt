package gov.cdc.ocio.processingstatusapi.functions.status

import com.azure.cosmos.models.CosmosQueryRequestOptions
import com.google.gson.GsonBuilder
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.ocio.processingstatusapi.cosmos.CosmosContainerManager
import gov.cdc.ocio.processingstatusapi.functions.traces.TraceUtils
import gov.cdc.ocio.processingstatusapi.model.*
import gov.cdc.ocio.processingstatusapi.model.reports.Report
import gov.cdc.ocio.processingstatusapi.model.reports.ReportDao
import gov.cdc.ocio.processingstatusapi.model.reports.ReportSerializer
import gov.cdc.ocio.processingstatusapi.model.traces.*
import gov.cdc.ocio.processingstatusapi.utils.JsonUtils
import mu.KotlinLogging
import java.util.*


/**
 * Collection of ways to get reports.
 *
 * @property request HttpRequestMessage<Optional<String>>
 * @property context ExecutionContext
 * @constructor
 */
class GetStatusFunction(
    private val request: HttpRequestMessage<Optional<String>>
) {

    private val logger = KotlinLogging.logger {}

    private val reportsContainerName = "Reports"
    private val partitionKey = "/uploadId"

    private val reportsContainer by lazy {
        CosmosContainerManager.initDatabaseContainer(reportsContainerName, partitionKey)!!
    }

    private val gson = GsonBuilder()
        .registerTypeAdapter(
            Report::class.java,
            ReportSerializer()
        )
        .registerTypeAdapter(
            Date::class.java,
            JsonUtils.GsonUTCDateAdapter()
        )
        .create()

    /**
     * Retrieve a complete status (traces + reports) for the provided upload ID.
     *
     * @param uploadId String
     * @return HttpResponseMessage
     */
    fun withUploadId(uploadId: String): HttpResponseMessage {

        val traces = TraceUtils.getTraces(uploadId)
        var traceResult: TraceResult? = null
        if (traces != null) {
            if (traces.size != 1) {
                return request
                    .createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Trace inconsistency found, expected exactly one trace for uploadId = $uploadId, but got ${traces.size}")
                    .build()
            }
            traceResult = TraceResult.buildFromTrace(traces[0])
        }

        val reportResult = getReport(uploadId)

        // Need at least one or the other (report or trace)
        if (traceResult == null && reportResult == null) {
            return request
                .createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body("Invalid uploadId provided")
                .build()
        }

        val status = StatusResult().apply {
            this.uploadId = traceResult?.uploadId
            this.destinationId = traceResult?.destinationId
            this.eventType = traceResult?.eventType
            trace = traceResult?.let { TraceDao.buildFromTraceResult(it) }
            reports = reportResult?.reports
        }

        return request
            .createResponseBuilder(HttpStatus.OK)
            .header("Content-Type", "application/json")
            .body(gson.toJson(status))
            .build()
    }

    companion object {
        val excludedSpanTags = listOf("spanMark", "span.kind", "internal.span.format", "otel.library.name")
    }

//    private fun getTraces(uploadId: String): TraceResult? {
//        val queryService: QueryServiceBlockingStub = jaeger.createBlockingQueryService()
//        WaitUtils.untilQueryHasTag(queryService, SERVICE_NAME, "foo", "bar")
//
//        val operations: GetOperationsResponse = queryService
//            .getOperations(GetOperationsRequest.newBuilder().setService(SERVICE_NAME).build())
//        Assert.assertEquals(1, operations.getOperationNamesList().size())
//        Assert.assertEquals(OPERATION_NAME, operations.getOperationNamesList().get(0))
//
//        return null
//    }

//    fun createBlockingQueryService(): QueryServiceBlockingStub {
//        val channel = ManagedChannelBuilder.forTarget(
//            java.lang.String.format(
//                "localhost:%d",
//                getQueryPort()
//            )
//        ).usePlaintext().build()
//        return QueryServiceGrpc.newBlockingStub(channel)
//    }
//
//    fun runFindTraces() {
//        val queryHostPort = "172.17.0.1:16686"
//        val channel = ManagedChannelBuilder.forTarget(queryHostPort).usePlaintext().build()
//        val queryService: QueryServiceBlockingStub = QueryServiceGrpc.newBlockingStub(channel)
//        val query = TraceQueryParameters.newBuilder().setServiceName("frontend")
//            .build()
//        val traceProto: Iterator<SpansResponseChunk> = queryService.findTraces(
//            FindTracesRequest.newBuilder().setQuery(query).build()
//        )
//        val protoSpans: MutableList<Span> = ArrayList<Span>()
//        while (traceProto.hasNext()) {
//            protoSpans.addAll(traceProto.next().getSpansList())
//        }
//        val traces: Trace = Converter.toModel(protoSpans)
//        val graph: Graph = GraphCreator.create(traces)
//        val errorSpans: Set<io.jaegertracing.analytics.model.Span> = NumberOfErrors.calculate(graph)
//        val result: MutableMap<String, MutableMap<String, Int>> = LinkedHashMap()
//        for (errorSpan in errorSpans) {
//            for (log in errorSpan.logs) {
//                val err: String = log.fields.get(Tags.ERROR.getKey())
//                if (err != null) {
//                    var traceIdCount = result[err]
//                    if (traceIdCount == null) {
//                        traceIdCount = LinkedHashMap()
//                        result[err] = traceIdCount
//                    }
//                    var count = traceIdCount[errorSpan.traceId]
//                    if (count == null) {
//                        count = 0
//                    }
//                    traceIdCount[errorSpan.traceId] = ++count
//                }
//            }
//        }
//        println("Error type, traceID and error count:")
//        for ((key, value) in result) {
//            System.out.printf("error type: %s\n", key)
//            for ((key1, value1) in value) {
//                System.out
//                    .printf("\tTraceID: %s, count %d\n", key1, value1)
//            }
//        }
//        val height: Int = TraceHeight.calculate(graph)
//        val networkLatencies: Map<Name, Set<Double>> = NetworkLatency.calculate(graph)
//        System.out.printf("Trace height = %d\n", height)
//        System.out.printf("Network latencies = %s\n", networkLatencies)
//    }

    private fun getReport(uploadId: String): ReportDao? {

        // Get the reports
        val reportsSqlQuery = "select * from $reportsContainerName r where r.uploadId = '$uploadId'"

        // Locate the existing report so we can amend it
        val reportItems = reportsContainer.queryItems(
            reportsSqlQuery, CosmosQueryRequestOptions(),
            Report::class.java
        )
        if (reportItems.count() > 0) {
            val report = reportItems.elementAt(0)

            val stageReportsSqlQuery = "select * from $reportsContainerName r where r.uploadId = '$uploadId'"

            // Locate the existing stage reports
            val stageReportItems = reportsContainer.queryItems(
                stageReportsSqlQuery, CosmosQueryRequestOptions(),
                Report::class.java
            )
            if (stageReportItems.count() > 0) {
                val stageReportItemList = stageReportItems.toList()

                logger.info("Successfully located report with uploadId = $uploadId")

                val reportResult = ReportDao().apply {
                    this.uploadId = uploadId
                    this.destinationId = report.destinationId
                    this.eventType = report.eventType
                    this.reports = stageReportItemList
                }
                return reportResult
            }
        }

        logger.error("Failed to locate report with uploadId = $uploadId")

        return null
    }

}