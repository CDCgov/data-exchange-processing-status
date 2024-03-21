package gov.cdc.ocio.processingstatusapi.functions.reports

import com.azure.cosmos.models.CosmosItemRequestOptions
import com.azure.cosmos.models.CosmosQueryRequestOptions
import com.azure.cosmos.models.PartitionKey
import com.azure.messaging.servicebus.ServiceBusMessage
import com.google.gson.GsonBuilder
import com.google.gson.ToNumberPolicy
import com.google.gson.reflect.TypeToken
import com.microsoft.applicationinsights.TelemetryClient
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.ocio.processingstatusapi.exceptions.BadRequestException
import gov.cdc.ocio.processingstatusapi.exceptions.BadStateException
import gov.cdc.ocio.processingstatusapi.exceptions.InvalidSchemaDefException
import gov.cdc.ocio.processingstatusapi.model.DispositionType
import gov.cdc.ocio.processingstatusapi.model.reports.NotificationReport
import gov.cdc.ocio.processingstatusapi.model.reports.Report
import gov.cdc.ocio.processingstatusapi.model.reports.Source
import gov.cdc.ocio.processingstatusapi.model.reports.stagereports.SchemaDefinition
import mu.KotlinLogging
import java.util.*

/**
 * The report manager interacts directly with CosmosDB to persist and retrieve reports.
 *
 * @property context ExecutionContext
 * @constructor
 */
class ReportManager {

    private val logger = KotlinLogging.logger {}

    private val telemetry = TelemetryClient()

    // Use the LONG_OR_DOUBLE number policy, which will prevent Longs from being made into Doubles
    private val gson = GsonBuilder()
        .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
        .create()

    /**
     * Create a report located with the provided upload ID.
     *
     * @param uploadId String
     * @param dataStreamId String
     * @param dataStreamRoute String
     * @param stageName String
     * @param contentType String
     * @param content String
     * @param dispositionType DispositionType
     * @return String - stage report identifier
     * @throws BadStateException
     * @throws BadRequestException
     */
    @Throws(BadStateException::class, BadRequestException::class)
    fun createReportWithUploadId(
        uploadId: String,
        dataStreamId: String,
        dataStreamRoute: String,
        stageName: String,
        contentType: String,
        messageId: String?,
        status: String?,
        content: String,
        dispositionType: DispositionType,
        source: Source
    ): String {
        // Verify the content contains the minimum schema information
        try {
            SchemaDefinition.fromJsonString(content)
        } catch(e: InvalidSchemaDefException) {
            throw BadRequestException("Invalid schema definition: ${e.localizedMessage}")
        } catch(e: Exception) {
            throw BadRequestException("Malformed message: ${e.localizedMessage}")
        }

        return createReport(uploadId, dataStreamId, dataStreamRoute, stageName, contentType, messageId, status, content, dispositionType, source)
    }

    /**
     * Create the provided report.  Note the dispositionType indicates whether this will add or replace existing
     * report(s) with this stageName.
     *
     * @param uploadId String
     * @param dataStreamId String
     * @param dataStreamRoute String
     * @param stageName String
     * @param contentType String
     * @param content String
     * @param dispositionType DispositionType - indicates whether to add or replace any existing reports for the
     * given stageName.
     * @return String - stage report identifier
     * */
    private fun createReport(uploadId: String,
                             dataStreamId: String,
                             dataStreamRoute: String,
                             stageName: String,
                             contentType: String,
                             messageId: String?,
                             status: String?,
                             content: String,
                             dispositionType: DispositionType,
                             source: Source): String {

        when (dispositionType) {
            DispositionType.REPLACE -> {
                logger.info("Replacing report(s) with stage name = $stageName")
                // Delete all stages matching the report ID with the same stage name
                val sqlQuery = "select * from ${reportMgrConfig.reportsContainerName} r where r.uploadId = '$uploadId' and r.stageName = '$stageName'"
                val items = reportMgrConfig.reportsContainer?.queryItems(
                    sqlQuery, CosmosQueryRequestOptions(),
                    Report::class.java
                )
                if ((items?.count() ?: 0) > 0) {
                    try {
                        items?.forEach {
                            reportMgrConfig.reportsContainer?.deleteItem(
                                it.id,
                                PartitionKey(it.uploadId),
                                CosmosItemRequestOptions()
                            )
                        }
                        logger.info("Removed all reports with stage name = $stageName")
                    } catch(e: Exception) {
                        throw BadStateException("Issue deleting report: ${e.localizedMessage}")
                    }
                }

                // Now create the new stage report
                return createStageReport(uploadId, dataStreamId, dataStreamRoute, stageName, contentType, messageId, status, content, source)
            }
            DispositionType.ADD -> {
                logger.info("Creating report for stage name = $stageName")
                return createStageReport(uploadId, dataStreamId, dataStreamRoute, stageName, contentType, messageId, status, content, source)
            }
        }
    }

    /**
     * Creates a report for the given stage.
     *
     * @param uploadId String
     * @param dataStreamId String
     * @param dataStreamRoute String
     * @param stageName String
     * @param contentType String
     * @param content String
     * @return String
     * @throws BadStateException
     */
    @Throws(BadStateException::class)
    private fun createStageReport(uploadId: String,
                                  dataStreamId: String,
                                  dataStreamRoute: String,
                                  stageName: String,
                                  contentType: String,
                                  messageId: String?,
                                  status: String?,
                                  content: String,
                                  source: Source): String {
        val stageReportId = UUID.randomUUID().toString()
        val stageReport = Report().apply {
            this.id = stageReportId
            this.uploadId = uploadId
            this.reportId = stageReportId
            this.dataStreamId = dataStreamId
            this.dataStreamRoute = dataStreamRoute
            this.stageName = stageName
            this.contentType = contentType
            this.messageId = messageId
            this.status = status

            if (contentType.lowercase() == "json") {
                val typeObject = object : TypeToken<HashMap<*, *>?>() {}.type
                val jsonMap: Map<String, Any> = gson.fromJson(content, typeObject)
                this.content = jsonMap
            } else
                this.content = content
        }

        telemetry.trackEvent("ReportCreateAttempt")
        reportMgrConfig.reportsContainer?.let { reportsContainer ->
            var attempts = 0
            do {
                try {
                    val response = reportsContainer.createItem(
                        stageReport,
                        PartitionKey(uploadId),
                        CosmosItemRequestOptions()
                    )

                    logger.info("Creating report, response http status code = ${response?.statusCode}, attempt = ${attempts + 1}, uploadId = $uploadId")
                    if (response != null) {
                        when (response.statusCode) {
                            HttpStatus.OK.value(), HttpStatus.CREATED.value() -> {
                                logger.info("Created report with reportId = ${response.item?.reportId}, uploadId = $uploadId")
                                telemetry.trackEvent("ReportCreatedSuccess")
                                val enableReportForwarding = System.getenv("EnableReportForwarding")
                                if (enableReportForwarding.equals("True", ignoreCase = true)) {
                                    //Send message to reports-notifications-queue
                                    val message = NotificationReport(
                                        response.item?.reportId,
                                        uploadId, dataStreamId, dataStreamRoute,
                                        stageName,
                                        contentType,
                                        content,
                                        messageId,
                                        status,
                                        source
                                    )
                                    reportMgrConfig.serviceBusSender.sendMessage(ServiceBusMessage(message.toString()))
                                }
                                return stageReportId
                            }

                            HttpStatus.TOO_MANY_REQUESTS.value() -> {
                                // See: https://learn.microsoft.com/en-us/azure/cosmos-db/nosql/performance-tips?tabs=trace-net-core#429
                                // https://learn.microsoft.com/en-us/rest/api/cosmos-db/common-cosmosdb-rest-response-headers
                                // https://learn.microsoft.com/en-us/azure/cosmos-db/nosql/troubleshoot-request-rate-too-large?tabs=resource-specific
                                val recommendedDuration = response.responseHeaders["x-ms-retry-after-ms"]
                                logger.warn("Received 429 (too many requests) from cosmossb, attempt ${attempts + 1}, will retry after $recommendedDuration millis, uploadId = $uploadId")
                                val waitMillis = recommendedDuration?.toLong()
                                Thread.sleep(waitMillis ?: DEFAULT_RETRY_INTERVAL_MILLIS)
                            }

                            else -> {
                                // Need to retry regardless
                                val retryAfterDurationMillis = getCalculatedRetryDuration(attempts)
                                logger.warn("Received response code ${response.statusCode}, attempt ${attempts + 1}, will retry after $retryAfterDurationMillis millis, uploadId = $uploadId")
                                Thread.sleep(retryAfterDurationMillis)
                            }
                        }
                    } else {
                        val retryAfterDurationMillis = getCalculatedRetryDuration(attempts)
                        logger.warn("Received null response from cosmosdb, attempt ${attempts + 1}, will retry after $retryAfterDurationMillis millis, uploadId = $uploadId")
                        Thread.sleep(retryAfterDurationMillis)
                    }
                } catch (e: Exception) {
                    val retryAfterDurationMillis = getCalculatedRetryDuration(attempts)
                    logger.error("CreateReport: Exception: ${e.localizedMessage}, attempt ${attempts + 1}, will retry after $retryAfterDurationMillis millis, uploadId = $uploadId")
                    Thread.sleep(retryAfterDurationMillis)
                }

            } while (attempts++ < MAX_RETRY_ATTEMPTS)

            telemetry.trackEvent("ReportCreatedFailed")
            throw BadStateException("Failed to create reportId = ${stageReport.reportId}, uploadId = $uploadId")
        }

        return ""
    }

    private fun getCalculatedRetryDuration(attempt: Int): Long {
        return DEFAULT_RETRY_INTERVAL_MILLIS * (attempt + 1)
    }

    companion object {
        const val DEFAULT_RETRY_INTERVAL_MILLIS = 500L
        const val MAX_RETRY_ATTEMPTS = 100
        val reportMgrConfig = ReportManagerConfig()
    }
}