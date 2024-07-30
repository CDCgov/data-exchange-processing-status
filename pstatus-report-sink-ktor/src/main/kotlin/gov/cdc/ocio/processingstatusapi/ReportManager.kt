package gov.cdc.ocio.processingstatusapi


import com.azure.cosmos.models.CosmosItemRequestOptions
import com.azure.cosmos.models.CosmosQueryRequestOptions
import com.azure.cosmos.models.PartitionKey
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.ToNumberPolicy
import com.google.gson.reflect.TypeToken
import gov.cdc.ocio.processingstatusapi.cosmos.CosmosDeadLetterRepository
import gov.cdc.ocio.processingstatusapi.cosmos.CosmosRepository
import gov.cdc.ocio.processingstatusapi.exceptions.BadRequestException
import gov.cdc.ocio.processingstatusapi.exceptions.BadStateException
import gov.cdc.ocio.processingstatusapi.models.DispositionType
import gov.cdc.ocio.processingstatusapi.models.Report
import gov.cdc.ocio.processingstatusapi.models.ReportDeadLetter
import gov.cdc.ocio.processingstatusapi.models.reports.MessageMetadata
import gov.cdc.ocio.processingstatusapi.models.reports.Source
import gov.cdc.ocio.processingstatusapi.models.reports.StageInfo
import gov.cdc.ocio.processingstatusapi.models.reports.Tags
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*
import io.netty.handler.codec.http.HttpResponseStatus


/**
 * The report manager interacts directly with CosmosDB to persist and retrieve reports.
 *
 * @constructor
 */
class ReportManager: KoinComponent {

    private val cosmosRepository by inject<CosmosRepository>()
    private val cosmosDeadLetterRepository by inject<CosmosDeadLetterRepository>()

    private val logger = KotlinLogging.logger {}

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
        messageMetadata: MessageMetadata?,
        stageInfo: StageInfo?,
        tags: Tags?,
        data:Map<String,String>?,
        contentType: String,
        messageId: String?,
        status: String?,
        content: Any?,
        jurisdiction: String?,
        senderId:String?,
        dispositionType: DispositionType,
        source: Source
    ): String {
        // Verify the content contains the minimum schema information
      /*  try {
            SchemaDefinition.fromJsonString(content)
        } catch(e: InvalidSchemaDefException) {
            throw BadRequestException("Invalid schema definition: ${e.localizedMessage}")
        } catch(e: Exception) {
            throw BadRequestException("Malformed message: ${e.localizedMessage}")
        }*/
        if (System.getProperty("isTestEnvironment") != "true") {
            return createReport(
                uploadId,
                dataStreamId,
                dataStreamRoute,
                messageMetadata,
                stageInfo,
                tags,
                data,
                contentType,
                messageId,
                status,
                content,
                jurisdiction,
                senderId,
                dispositionType,
                source
            )
        }
        return uploadId // this is just as a fallback
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
                             messageMetadata: MessageMetadata?,
                             stageInfo: StageInfo?,
                             tags: Tags?,
                             data:Map<String,String>?,
                             contentType: String,
                             messageId: String?,
                             status: String?,
                             content: Any?,
                             jurisdiction: String?,
                             senderId:String?,
                             dispositionType: DispositionType,
                             source: Source): String {

        when (dispositionType) {
            DispositionType.REPLACE -> {
                logger.info("Replacing report(s) with stage name = ${stageInfo?.stage}")
                // Delete all stages matching the report ID with the same stage name
                val sqlQuery = "select * from ${reportMgrConfig.reportsContainerName} r where r.uploadId = '$uploadId' and r.stageName = '$stageInfo?.stage'"
                val items = cosmosRepository.reportsContainer?.queryItems(
                    sqlQuery, CosmosQueryRequestOptions(),
                    Report::class.java
                )
                if ((items?.count() ?: 0) > 0) {
                    try {
                        items?.forEach {
                            cosmosRepository.reportsContainer?.deleteItem(
                                it.id,
                                PartitionKey(it.uploadId),
                                CosmosItemRequestOptions()
                            )
                        }
                        logger.info("Removed all reports with stage name = $stageInfo?.stage")
                    } catch(e: Exception) {
                        throw BadStateException("Issue deleting report: ${e.localizedMessage}")
                    }
                }

                // Now create the new stage report
                return createStageReport(uploadId, dataStreamId, dataStreamRoute, messageMetadata,stageInfo, tags, data, contentType, messageId, status, content, jurisdiction,senderId, source)
            }
            DispositionType.ADD -> {
                logger.info("Creating report for stage name = $stageInfo?.stage")
                return createStageReport(uploadId, dataStreamId, dataStreamRoute, messageMetadata, stageInfo,tags,data, contentType, messageId, status, content, jurisdiction,senderId, source)
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
                                  messageMetadata: MessageMetadata?,
                                  stageInfo: StageInfo?,
                                  tags: Tags?,
                                  data:Map<String,String>?,
                                  contentType: String,
                                  messageId: String?,
                                  status: String?,
                                  content: Any?,
                                  jurisdiction: String?,
                                  senderId:String?,
                                  source: Source): String {
        val stageReportId = UUID.randomUUID().toString()
        val stageReport = Report().apply {
            this.id = stageReportId
            this.uploadId = uploadId
            this.reportId = stageReportId
            this.dataStreamId = dataStreamId
            this.dataStreamRoute = dataStreamRoute
            this.jurisdiction= jurisdiction
            this.senderId= senderId
            this.messageMetadata = messageMetadata
            this.stageInfo= stageInfo
            this.tags= tags
            this.data= data
            this.jurisdiction= jurisdiction
            this.senderId= senderId
            this.contentType = contentType
            this.messageId = messageId
            this.status = status

            if (contentType.lowercase() == "json") {
                val typeObject = object : TypeToken<HashMap<*, *>?>() {}.type
                val jsonMap: Map<String, Any> = gson.fromJson(Gson().toJson(content, MutableMap::class.java).toString(), typeObject)
                this.content = jsonMap
            } else
                this.content = content
        }
       return createReportItem(uploadId,stageReportId,stageReport)
    }

    /**
     * Creates a dead-letter report if there is a malformed data or missing required fields
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
    fun createDeadLetterReport(uploadId: String?,
                               dataStreamId: String?,
                               dataStreamRoute: String?,
                               messageMetadata: MessageMetadata?,
                               stageInfo: StageInfo?,
                               tags: Tags?,
                               data:Map<String,String>?,
                               dispositionType: DispositionType,
                               contentType: String?,
                               content: Any?,
                               jurisdiction: String?,
                               senderId:String?,
                               deadLetterReasons: List<String>,
                               validationSchemaFileNames:List<String>
    ): String {

        val deadLetterReportId = UUID.randomUUID().toString()
        val deadLetterReport = ReportDeadLetter().apply {
            this.id = deadLetterReportId
            this.uploadId = uploadId
            this.reportId = deadLetterReportId
            this.dataStreamId = dataStreamId
            this.dataStreamRoute = dataStreamRoute
            this.jurisdiction= jurisdiction
            this.senderId= senderId
            this.messageMetadata= messageMetadata
            this.stageInfo= stageInfo
            this.tags= tags
            this.data= data
            this.jurisdiction= jurisdiction
            this.senderId= senderId
            this.dispositionType= dispositionType.toString()
            this.contentType = contentType
            this.deadLetterReasons= deadLetterReasons
            this.validationSchemas= validationSchemaFileNames
            if (contentType?.lowercase() == "json" && !isNullOrEmpty(content) && !isBase64Encoded(content.toString())) {
                val typeObject = object : TypeToken<HashMap<*, *>?>() {}.type
                val jsonMap: Map<String, Any> = gson.fromJson(Gson().toJson(content, MutableMap::class.java).toString(), typeObject)
                this.content = jsonMap
            } else
                this.content = content
        }
        return  createReportItem(uploadId,deadLetterReportId,deadLetterReport)
    }

    /**
     * Creates a dead-letter report if there is a malformed JSON. This is called if DISABLE_VALIDATION is true
     *
     * @param deadLetterReason String
     * @return String
     * @throws BadStateException
     */

    @Throws(BadStateException::class)
    fun createDeadLetterReport(deadLetterReason: String): String {
        val deadLetterReportId = UUID.randomUUID().toString()
        val deadLetterReport = ReportDeadLetter().apply {
            this.id = deadLetterReportId
            this.deadLetterReasons = listOf(deadLetterReason)
        }
        return  createReportItem(null,deadLetterReportId,deadLetterReport)
    }

    /**
     * The function which calculates the interval after which the retry should occur
     * @param attempt Int
     */
    private fun getCalculatedRetryDuration(attempt: Int): Long {
        return DEFAULT_RETRY_INTERVAL_MILLIS * (attempt + 1)
    }
    /** Function to check whether the value is null or empty based on its type
     * @param value Any
     */
    private fun isNullOrEmpty(value: Any?): Boolean {
        return when (value) {
            null -> true
            is String -> value.isEmpty()
            is Collection<*> -> value.isEmpty()
            is Map<*, *> -> value.isEmpty()
            else -> false // You can adjust this to your needs
        }
    }

    /**
     * The function which checks whether the passed string is Base64 Encoded or not using Regex
     * @param value String
     */
    private fun isBase64Encoded(value: String): Boolean {
        val base64Pattern = "^[A-Za-z0-9+/]+={0,2}$"
        return value.matches(base64Pattern.toRegex())
    }

    /**
     * The common function which writes to cosmos container based on the report type
     * @param uploadId String
     * @param reportId String
     * @reportType Any
     */

    private fun  createReportItem(uploadId: String?, reportId:String, reportType:Any) : String{

        var responseReportId = ""
        var reportTypeName = "Report"
        var statusCode:Int? = null
        var isValidResponse = false
        var recommendedDuration:String?= null
        var attempts = 0

        do {
            try {
                //use when here to determing whether report type is StageReport or DeadLetterReport
                when (reportType) {
                    is ReportDeadLetter -> {
                        val response = cosmosDeadLetterRepository.reportsDeadLetterContainer?.createItem(
                            reportType,
                            PartitionKey(uploadId),
                            CosmosItemRequestOptions())

                        isValidResponse = response!=null
                        reportTypeName ="dead-letter report"
                        responseReportId = response?.item?.reportId ?: "0"
                        statusCode = response?.statusCode
                        recommendedDuration = response?.responseHeaders?.get("x-ms-retry-after-ms")
                    }

                    is Report -> {
                        val response = cosmosRepository.reportsContainer?.createItem(
                            reportType,
                            PartitionKey(uploadId),
                            CosmosItemRequestOptions())

                        isValidResponse = response!=null
                        responseReportId = response?.item?.reportId ?: "0"
                        statusCode = response?.statusCode
                        recommendedDuration = response?.responseHeaders?.get("x-ms-retry-after-ms")

                    }

                }
                logger.info("Creating ${reportTypeName}, response http status code = ${statusCode}, attempt = ${attempts + 1}, uploadId = $uploadId")
                  if(isValidResponse){

                      when (statusCode) {
                          HttpResponseStatus.OK.code(), HttpResponseStatus.CREATED.code() -> {
                              logger.info("Created report with reportId = ${responseReportId}, uploadId = $uploadId")
                              return reportId
                          }

                          HttpResponseStatus.TOO_MANY_REQUESTS.code() -> {
                              // See: https://learn.microsoft.com/en-us/azure/cosmos-db/nosql/performance-tips?tabs=trace-net-core#429
                              // https://learn.microsoft.com/en-us/rest/api/cosmos-db/common-cosmosdb-rest-response-headers
                              // https://learn.microsoft.com/en-us/azure/cosmos-db/nosql/troubleshoot-request-rate-too-large?tabs=resource-specific

                              logger.warn("Received 429 (too many requests) from cosmosdb, attempt ${attempts + 1}, will retry after $recommendedDuration millis, uploadId = $uploadId")
                              val waitMillis = recommendedDuration?.toLong()
                              Thread.sleep(waitMillis ?: DEFAULT_RETRY_INTERVAL_MILLIS)
                          }

                          else -> {
                              // Need to retry regardless
                              val retryAfterDurationMillis = getCalculatedRetryDuration(attempts)
                              logger.warn("Received response code ${statusCode}, attempt ${attempts + 1}, will retry after $retryAfterDurationMillis millis, uploadId = $uploadId")
                              Thread.sleep(retryAfterDurationMillis)
                          }
                      }
                    }
                  else {
                      val retryAfterDurationMillis = getCalculatedRetryDuration(attempts)
                      logger.warn("Received null response from cosmosdb, attempt ${attempts + 1}, will retry after $retryAfterDurationMillis millis, uploadId = $uploadId")
                      Thread.sleep(retryAfterDurationMillis)
                  }
            }
            catch (e: Exception) {
                val retryAfterDurationMillis = getCalculatedRetryDuration(attempts)
                logger.error("CreateReport: Exception: ${e.localizedMessage}, attempt ${attempts + 1}, will retry after $retryAfterDurationMillis millis, uploadId = $uploadId")
                Thread.sleep(retryAfterDurationMillis)
            }



        } while (attempts++ < MAX_RETRY_ATTEMPTS)
        throw BadStateException("Failed to create dead-letterReport reportId = ${responseReportId}, uploadId = $uploadId")
    }

    companion object {
        const val DEFAULT_RETRY_INTERVAL_MILLIS = 500L
        const val MAX_RETRY_ATTEMPTS = 100
        val reportMgrConfig = ReportManagerConfig()
    }
}