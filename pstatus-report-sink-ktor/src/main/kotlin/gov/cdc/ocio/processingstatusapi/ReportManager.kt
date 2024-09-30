package gov.cdc.ocio.processingstatusapi

import com.google.gson.reflect.TypeToken
import gov.cdc.ocio.processingstatusapi.exceptions.BadRequestException
import gov.cdc.ocio.processingstatusapi.exceptions.BadStateException
import gov.cdc.ocio.processingstatusapi.models.DispositionType
import gov.cdc.ocio.database.models.Report
import gov.cdc.ocio.database.models.ReportDeadLetter
import gov.cdc.ocio.processingstatusapi.models.MessageMetadataSB
import gov.cdc.ocio.processingstatusapi.models.Source
import gov.cdc.ocio.processingstatusapi.models.StageInfoSB
import gov.cdc.ocio.database.persistence.ProcessingStatusRepository
import mu.KotlinLogging
import org.apache.commons.lang3.SerializationUtils
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Instant
import java.util.*


/**
 * The report manager interacts directly with CosmosDB to persist and retrieve reports.
 *
 * @constructor
 */
class ReportManager: KoinComponent {

    private val repository by inject<ProcessingStatusRepository>()

    private val logger = KotlinLogging.logger {}

    /**
     * Create a report located with the provided upload ID.
     *
     * @param uploadId String
     * @param dataStreamId String
     * @param dataStreamRoute String
     * @param dexIngestDateTime Instant
     * @param messageMetadata MessageMetadata?
     * @param stageInfo StageInfo?
     * @param tags Map<String, String>?
     * @param data Map<String, String>?
     * @param contentType String
     * @param content Any?
     * @param jurisdiction String?
     * @param senderId String?
     * @param dispositionType DispositionType
     * @param source Source
     * @return String
     * @throws BadStateException
     * @throws BadRequestException
     */
    @Throws(BadStateException::class, BadRequestException::class)
    fun createReportWithUploadId(
        uploadId: String,
        dataStreamId: String,
        dataStreamRoute: String,
        dexIngestDateTime: Instant,
        messageMetadata: MessageMetadataSB?,
        stageInfo: StageInfoSB?,
        tags: Map<String, String>?,
        data: Map<String, String>?,
        contentType: String,
        content: Any?,
        jurisdiction: String?,
        senderId: String?,
        dispositionType: DispositionType,
        source: Source
    ): String {
        if (System.getProperty("isTestEnvironment") != "true") {
            return createReport(
                uploadId,
                dataStreamId,
                dataStreamRoute,
                dexIngestDateTime,
                messageMetadata,
                stageInfo,
                tags,
                data,
                contentType,
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
     * @param dexIngestDateTime Instant
     * @param messageMetadata MessageMetadata?
     * @param stageInfo StageInfo?
     * @param tags Map<String, String>?
     * @param data Map<String, String>?
     * @param contentType String
     * @param content Any?
     * @param jurisdiction String?
     * @param senderId String?
     * @param dispositionType DispositionType - indicates whether to add or replace any existing reports for the given stageName.
     * @param source Source
     * @return String - report identifier
     */
    private fun createReport(uploadId: String,
                             dataStreamId: String,
                             dataStreamRoute: String,
                             dexIngestDateTime: Instant,
                             messageMetadata: MessageMetadataSB?,
                             stageInfo: StageInfoSB?,
                             tags: Map<String, String>?,
                             data: Map<String, String>?,
                             contentType: String,
                             content: Any?,
                             jurisdiction: String?,
                             senderId:String?,
                             dispositionType: DispositionType,
                             source: Source
    ): String {

        when (dispositionType) {
            DispositionType.REPLACE -> {
                logger.info("Replacing report(s) with service = '${stageInfo?.service}', action = '${stageInfo?.action}'")
                // Delete all stages matching the report ID with the same service and action name
                val cName = repository.reportsCollection.collectionNameForQuery
                val cVar = repository.reportsCollection.collectionVariable
                val cPrefix = repository.reportsCollection.collectionVariablePrefix
                val cElFunc = repository.reportsCollection.collectionElementForQuery
                val sqlQuery = (
                        "select * from $cName $cVar "
                                + "where ${cPrefix}uploadId = '$uploadId' "
                                + "and ${cPrefix}stageInfo.${cElFunc("service")} = '${stageInfo?.service}' "
                                + "and ${cPrefix}stageInfo.${cElFunc("action")} = '${stageInfo?.action}'"
                        )
                val items = repository.reportsCollection.queryItems(
                    sqlQuery,
                    Report::class.java
                )
                if (items.isNotEmpty()) {
                    try {
                        items.forEach {
                            repository.reportsCollection.deleteItem(
                                it.id,
                                it.uploadId
                            )
                        }
                        logger.info("Removed all reports with stage name = $stageInfo?.stage")
                    } catch(e: Exception) {
                        throw BadStateException("Issue deleting report: ${e.localizedMessage}")
                    }
                }

                // Now create the new stage report
                return createStageReport(
                    uploadId,
                    dataStreamId,
                    dataStreamRoute,
                    dexIngestDateTime,
                    messageMetadata,
                    stageInfo,
                    tags,
                    data,
                    contentType,
                    content,
                    jurisdiction,
                    senderId,
                    source
                )
            }
            DispositionType.ADD -> {
                logger.info("Creating report for stage name = $stageInfo?.stage")
                return createStageReport(
                    uploadId,
                    dataStreamId,
                    dataStreamRoute,
                    dexIngestDateTime,
                    messageMetadata,
                    stageInfo,
                    tags,
                    data,
                    contentType,
                    content,
                    jurisdiction,
                    senderId,
                    source
                )
            }
        }
    }

    /**
     * Creates a report for the given stage.
     *
     * @param uploadId String
     * @param dataStreamId String
     * @param dataStreamRoute String
     * @param dexIngestDateTime Instant
     * @param messageMetadata MessageMetadata?
     * @param stageInfo StageInfo?
     * @param tags Map<String, String>?
     * @param data Map<String, String>?
     * @param contentType String
     * @param content Any?
     * @param jurisdiction String?
     * @param senderId String?
     * @param source Source
     * @return String - report identifier
     * @throws BadStateException
     */
    @Throws(BadStateException::class)
    private fun createStageReport(uploadId: String,
                                  dataStreamId: String,
                                  dataStreamRoute: String,
                                  dexIngestDateTime: Instant,
                                  messageMetadata: MessageMetadataSB?,
                                  stageInfo: StageInfoSB?,
                                  tags: Map<String, String>?,
                                  data: Map<String, String>?,
                                  contentType: String,
                                  content: Any?,
                                  jurisdiction: String?,
                                  senderId: String?,
                                  source: Source
    ): String {
        val stageReportId = UUID.randomUUID().toString()
        val stageReport = Report().apply {
            this.id = stageReportId
            this.uploadId = uploadId
            this.reportId = stageReportId
            this.dataStreamId = dataStreamId
            this.dataStreamRoute = dataStreamRoute
            this.dexIngestDateTime = dexIngestDateTime
            this.jurisdiction = jurisdiction
            this.senderId = senderId
            this.messageMetadata = messageMetadata?.toMessageMetadata()
            this.stageInfo = stageInfo?.toStageInfo()
            this.tags = tags
            this.data = data
            this.jurisdiction = jurisdiction
            this.senderId = senderId
            this.contentType = contentType
            this.content = getContent(contentType, content)
        }
       return createReportItem(uploadId, stageReportId, stageReport)
    }

    /**
     * Creates a dead-letter report if there is a malformed data or missing required fields
     *
     * @param uploadId String?
     * @param dataStreamId String?
     * @param dataStreamRoute String?
     * @param dexIngestDateTime Instant?
     * @param messageMetadata MessageMetadataSB?
     * @param stageInfo StageInfoSB?
     * @param tags Map<String, String>?
     * @param data Map<String, String>?
     * @param dispositionType DispositionType
     * @param contentType String?
     * @param content Any?
     * @param jurisdiction String?
     * @param senderId String?
     * @param deadLetterReasons List<String>
     * @param validationSchemaFileNames List<String>
     * @return String
     * @throws BadStateException
     */
    @Throws(BadStateException::class)
    fun createDeadLetterReport(uploadId: String?,
                               dataStreamId: String?,
                               dataStreamRoute: String?,
                               dexIngestDateTime: Instant?,
                               messageMetadata: MessageMetadataSB?,
                               stageInfo: StageInfoSB?,
                               tags: Map<String,String>?,
                               data: Map<String,String>?,
                               dispositionType: DispositionType,
                               contentType: String?,
                               content: Any?,
                               jurisdiction: String?,
                               senderId:String?,
                               deadLetterReasons: List<String>,
                               validationSchemaFileNames: List<String>
    ): String {

        val deadLetterReportId = UUID.randomUUID().toString()
        val deadLetterReport = ReportDeadLetter().apply {
            this.id = deadLetterReportId
            this.uploadId = uploadId
            this.reportId = deadLetterReportId
            this.dataStreamId = dataStreamId
            this.dataStreamRoute = dataStreamRoute
            this.dexIngestDateTime = dexIngestDateTime
            this.jurisdiction = jurisdiction
            this.senderId = senderId
            this.messageMetadata = messageMetadata?.toMessageMetadata()
            this.stageInfo = stageInfo?.toStageInfo()
            this.tags = tags
            this.data = data
            this.jurisdiction = jurisdiction
            this.senderId = senderId
            this.dispositionType = dispositionType.toString()
            this.contentType = contentType
            this.deadLetterReasons= deadLetterReasons
            this.validationSchemas= validationSchemaFileNames
            this.content = getContent(contentType, content)
        }

        return createReportItem(uploadId, deadLetterReportId, deadLetterReport)
    }

    /**
     * Attempt to determine the content and transform it to the type needed by the database.  If the content is not
     * JSON then it will be persisted as a base64 encoded string.
     *
     * @param contentType String?
     * @param content Any?
     * @return Any?
     */
    private fun getContent(contentType: String?, content: Any?): Any? {
        if (contentType?.lowercase() == "application/json" || contentType?.lowercase() == "json") {
            try {
                if (content is Map<*, *>)
                    return repository.contentTransformer(content)
                else
                    throw BadStateException("Failed to interpret provided content as a JSON string")
            } catch (e: Exception) {
                logger.error { e.localizedMessage }
                return null
            }
        } else {
            val bytesArray = SerializationUtils.serialize(content as String)
            return Base64.getEncoder().encodeToString(bytesArray)
        }
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
        return createReportItem(null, deadLetterReportId, deadLetterReport)
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
     *
     * @param uploadId String?
     * @param reportId String
     * @param reportType Any
     * @return String - report identifier
     */
    private fun createReportItem(uploadId: String?, reportId: String, reportType: Any): String {
        var success = false

        // Determine whether report type is Report or ReportDeadLetter
        when (reportType) {
            is ReportDeadLetter -> {
                success = repository.reportsDeadLetterCollection.createItem(
                    reportId,
                    reportType,
                    Report::class.java,
                    uploadId
                )
            }

            is Report -> {
                success = repository.reportsCollection.createItem(
                    reportId,
                    reportType,
                    Report::class.java,
                    uploadId
                )
            }
        }

        if (success)
            return reportId

        throw BadStateException("Failed to create ${reportType::class.simpleName} with reportId = reportId, uploadId = $uploadId")
    }
}